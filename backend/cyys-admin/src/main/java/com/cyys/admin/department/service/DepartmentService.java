package com.cyys.admin.department.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.department.dto.DepartmentDTO;
import com.cyys.admin.department.dto.DepartmentDTO.Create;
import com.cyys.admin.department.dto.DepartmentDTO.VersionedId;
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
import java.util.List;

/** 受保护的单位部门资源；查询和写入均由 Mapper 数据策略限定。 */
@Service
@Validated
@RequiredArgsConstructor
public class DepartmentService {
    private final SysOrgMapper mapper;
    private final DataAuditService audit;

    @Transactional(readOnly = true)
    public IPage<DepartmentDTO.View> page(@Valid DepartmentDTO.Query query) {
        Page<SysOrg> page = new Page<>(query.page(), query.size());
        page.setOptimizeCountSql(false);
        var wrapper = new QueryWrapper<SysOrg>().like(query.search() != null && !query.search().isBlank(), "name", query.search())
                .orderByAsc("sort", "id");
        return mapper.selectPage(page, wrapper).convert(DepartmentDTO.View::from);
    }

    @Transactional(readOnly = true)
    public DepartmentDTO.View detail(String id) {
        SysOrg org = mapper.selectById(id);
        if (org == null) throw new ApiException(404, "记录不存在或不可访问");
        return DepartmentDTO.View.from(org);
    }

    @Transactional(readOnly = true)
    public List<DepartmentDTO.Option> parentOptionsForCreate() {
        return mapper.parentOptionsForCreate();
    }

    @Transactional(readOnly = true)
    public List<DepartmentDTO.Option> treeOptionsForRead() {
        return mapper.treeOptionsForRead();
    }

    @Transactional(readOnly = true)
    public List<DepartmentDTO.View> acrossAuthorizedScopes() {
        return mapper.selectAcrossAuthorizedScopes().stream().map(DepartmentDTO.View::from).toList();
    }

    @Transactional
    public DepartmentDTO.View create(@Valid DepartmentDTO.Create command) { return insert(command); }

    @Transactional
    public List<DepartmentDTO.View> createBatch(@NotEmpty @Size(max=100) List<@NotNull @Valid Create> commands) {
        return commands.stream().map(this::insert).toList();
    }

    private DepartmentDTO.View insert(DepartmentDTO.Create command) {
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
        String ancestors = parent == null ? "" : (parent.getAncestors().isEmpty() ? parent.getId() : parent.getAncestors() + "," + parent.getId());
        if (ancestors.length() > 500) throw new ApiException(409, "部门层级过深");
        org.setAncestors(ancestors);
        try {
            requireOne(mapper.insert(org));
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(409, "部门编码或关联约束冲突");
        }
        audit.success("department", org.getId(), "CREATE");
        return DepartmentDTO.View.from(org);
    }

    @Transactional
    public DepartmentDTO.View edit(@Valid DepartmentDTO.Edit command) {
        SysOrg org = mapper.selectForUpdate(command.id());
        if (org == null || !org.getVersion().equals(command.version())) throw new ApiException(409, "记录不可修改或版本已变更");
        org.setName(command.name());
        requireOne(mapper.updateById(org));
        audit.success("department", org.getId(), "UPDATE");
        return DepartmentDTO.View.from(org);
    }

    @Transactional
    public void deleteBatch(@NotEmpty @Size(max=100) List<@NotNull @Valid VersionedId> commands) {
        List<String> ids = commands.stream().map(DepartmentDTO.VersionedId::id).distinct().toList();
        if (ids.size() != commands.size()) throw new ApiException(400, "批量记录不能重复");
        var records = mapper.selectForDelete(ids).stream().collect(java.util.stream.Collectors.toMap(SysOrg::getId, org -> org));
        if (records.size() != ids.size()) throw new ApiException(409, "批次包含不可操作的记录");
        for (var command : commands) {
            if (!records.get(command.id()).getVersion().equals(command.version())) throw new ApiException(409, "批次包含已变更的记录");
            if (mapper.hasActiveReferences(command.id(), ScopeContext.get().requireScopeId())) throw new ApiException(409, "批次包含仍被引用的部门");
        }
        for (var command : commands) {
            requireOne(mapper.softDelete(command.id(), command.version(), ScopeContext.get().loginId()));
            audit.success("department", command.id(), "DELETE");
        }
    }

    private static void requireOne(int count) {
        if (count != 1) throw new ApiException(409, "记录不可操作、有关联依赖或版本已变更");
    }
}
