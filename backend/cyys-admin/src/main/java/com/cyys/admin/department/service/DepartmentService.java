package com.cyys.admin.department.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.cache.service.BusinessCacheGroups;
import com.cyys.common.cache.CacheGroupService;
import com.cyys.admin.department.dto.UpdateDepartmentCommand;
import com.cyys.admin.department.dto.DepartmentQuery;
import com.cyys.admin.department.dto.DepartmentOption;
import com.cyys.admin.department.dto.DepartmentLeaderOption;
import com.cyys.admin.department.dto.DepartmentView;
import com.cyys.admin.department.dto.CreateDepartmentCommand;
import com.cyys.admin.department.dto.DepartmentVersionedId;
import com.cyys.admin.department.mapper.DepartmentLeaderMapper;
import com.cyys.admin.department.mapper.DepartmentReferenceMapper;
import com.cyys.admin.department.mapper.SysOrgMapper;
import com.cyys.admin.department.model.SysOrg;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.util.*;

/** 受保护的单位部门资源；查询和写入均由 Mapper 数据策略限定。 */
@Service
@Validated
@RequiredArgsConstructor
public class DepartmentService {
    private final SysOrgMapper mapper;
    private final DepartmentLeaderMapper leaders;
    private final DepartmentReferenceMapper references;
    private final DataAuditService audit;
    private final CacheGroupService caches;

    @Transactional(readOnly = true)
    public IPage<DepartmentView> page(@Valid DepartmentQuery query) {
        Page<SysOrg> page = new Page<>(query.page(), query.size());
        page.setOptimizeCountSql(false);
        var wrapper = new QueryWrapper<SysOrg>().like(query.search() != null && !query.search().isBlank(), "name", query.search())
                .orderByAsc("sort", "id");
        IPage<SysOrg> result = mapper.selectPage(page, wrapper);
        return result.convert(DepartmentView::from);
    }

    @Transactional(readOnly = true)
    public DepartmentView detail(String id) {
        SysOrg org = mapper.selectById(id);
        if (org == null) throw new ApiException(404, "记录不存在或不可访问");
        return DepartmentView.from(org);
    }

    @Transactional(readOnly = true)
    public List<DepartmentOption> parentOptionsForCreate() {
        return mapper.parentOptionsForCreate();
    }

    @Transactional(readOnly = true)
    public List<DepartmentOption> treeOptionsForRead() {
        return mapper.treeOptionsForRead();
    }

    @Transactional(readOnly = true)
    public List<DepartmentLeaderOption> leaderOptionsForCreate(@Size(max = 100) String search) {
        return leaders.activeOptions(ScopeContext.get().requireScopeId(), search);
    }

    @Transactional(readOnly = true)
    public List<DepartmentLeaderOption> leaderOptionsForUpdate(String id, @Size(max = 100) String search) {
        SysOrg org = mapper.selectForUpdateOptions(id);
        if (org == null) throw new ApiException(404, "记录不存在或不可修改");
        List<DepartmentLeaderOption> options = new ArrayList<>(
                leaders.activeOptions(ScopeContext.get().requireScopeId(), search));
        if (org.getLeaderId() != null && options.stream().noneMatch(item -> item.value().equals(org.getLeaderId()))) {
            DepartmentLeaderOption historical = leaders.historicalOption(org.getScopeId(), org.getLeaderId());
            options.addFirst(historical == null
                    ? new DepartmentLeaderOption(org.getLeaderId(), "已失效成员（" + org.getLeaderId() + "）", true)
                    : historical);
        }
        return List.copyOf(options);
    }

    @Transactional(readOnly = true)
    public List<DepartmentView> acrossAuthorizedScopes() {
        return mapper.selectAcrossAuthorizedScopes().stream().map(DepartmentView::from).toList();
    }

    @Transactional
    public DepartmentView create(@Valid CreateDepartmentCommand command) { return insert(command); }

    @Transactional
    public List<DepartmentView> createBatch(@NotEmpty @Size(max=100) List<@NotNull @Valid CreateDepartmentCommand> commands) {
        return commands.stream().map(this::insert).toList();
    }

    private DepartmentView insert(CreateDepartmentCommand command) {
        var identity = ScopeContext.get();
        SysOrg parent = null;
        if (command.parentId() != null) {
            parent = mapper.parentForCreate(command.parentId());
            if (parent == null || !parent.getScopeId().equals(identity.requireScopeId())) throw new ApiException(403, "关联部门不可用");
        }
        var org = new SysOrg();
        org.setId(IdWorker.getIdStr());
        org.setScopeId(identity.requireScopeId());
        org.setName(command.name());
        org.setCode(command.code());
        org.setParentId(parent == null ? null : parent.getId());
        if (command.leaderId() != null) requireEligibleLeader(identity.requireScopeId(), command.leaderId());
        org.setLeaderId(command.leaderId());
        String ancestors = parent == null ? "" : (parent.getAncestors().isEmpty() ? parent.getId() : parent.getAncestors() + "," + parent.getId());
        if (ancestors.length() > 500) throw new ApiException(409, "部门层级过深");
        org.setAncestors(ancestors);
        try {
            requireOne(mapper.insert(org));
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(409, "部门编码或关联约束冲突");
        }
        audit.success("department", org.getId(), "CREATE");
        caches.invalidateAfterCommit(BusinessCacheGroups.DEPARTMENTS);
        return DepartmentView.from(org);
    }

    @Transactional
    public DepartmentView edit(@Valid UpdateDepartmentCommand command) {
        SysOrg org = mapper.selectForUpdate(command.id());
        if (org == null || !org.getVersion().equals(command.version())) throw new ApiException(409, "记录不可修改或版本已变更");
        if (command.leaderId() != null && !Objects.equals(command.leaderId(), org.getLeaderId())) {
            requireEligibleLeader(org.getScopeId(), command.leaderId());
        }
        org.setName(command.name());
        org.setLeaderId(command.leaderId());
        requireOne(mapper.updateById(org));
        audit.success("department", org.getId(), "UPDATE");
        caches.invalidateAfterCommit(BusinessCacheGroups.DEPARTMENTS);
        return DepartmentView.from(org);
    }

    @Transactional
    public void deleteBatch(@NotEmpty @Size(max=100) List<@NotNull @Valid DepartmentVersionedId> commands) {
        List<String> ids = commands.stream().map(DepartmentVersionedId::id).distinct().toList();
        if (ids.size() != commands.size()) throw new ApiException(400, "批量记录不能重复");
        var records = mapper.selectForDelete(ids).stream().collect(java.util.stream.Collectors.toMap(SysOrg::getId, org -> org));
        if (records.size() != ids.size()) throw new ApiException(409, "批次包含不可操作的记录");
        for (var command : commands) {
            if (!records.get(command.id()).getVersion().equals(command.version())) throw new ApiException(409, "批次包含已变更的记录");
            if (references.hasActiveReferences(command.id(), ScopeContext.get().requireScopeId())) throw new ApiException(409, "批次包含仍被引用的部门");
        }
        for (var command : commands) {
            requireOne(mapper.softDelete(command.id(), command.version(), ScopeContext.get().loginId()));
            audit.success("department", command.id(), "DELETE");
        }
        caches.invalidateAfterCommit(BusinessCacheGroups.DEPARTMENTS);
    }

    private static void requireOne(int count) {
        if (count != 1) throw new ApiException(409, "记录不可操作、有关联依赖或版本已变更");
    }

    private void requireEligibleLeader(String scope, String leaderId) {
        if (leaders.eligibleForShare(scope, leaderId) == null) throw new ApiException(403, "负责人不是当前单位的有效成员");
    }
}
