package com.cyys.admin.cache.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.auth.mapper.DataAuthorizationMapper;
import com.cyys.admin.cache.dto.CacheDefinitionView;
import com.cyys.admin.cache.dto.CreateCacheDefinitionCommand;
import com.cyys.admin.cache.dto.UpdateCacheDefinitionCommand;
import com.cyys.admin.cache.mapper.CacheDefinitionMapper;
import com.cyys.admin.cache.model.CacheDefinition;
import com.cyys.common.cache.CacheGroup;
import com.cyys.common.cache.CacheGroupService;
import com.cyys.common.cache.CacheRegion;
import com.cyys.common.cache.CacheScope;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CacheManagementService {
    private final CacheDefinitionMapper mapper;
    private final CacheGroupService groups;
    private final DataAuthorizationMapper authorization;
    private final DataAuditService audit;
    private final TransactionTemplate transactions;

    public List<CacheDefinitionView> list() {
        requirePermission();
        return mapper.list().stream().map(CacheDefinitionView::from).toList();
    }

    @Transactional
    public CacheDefinitionView create(CreateCacheDefinitionCommand command) {
        requirePermission();
        var row = new CacheDefinition();
        row.setId(IdWorker.getIdStr());
        fill(row, command.name(), command.cacheKey(), command.groupName(), command.refreshGroup(), command.scope(), command.description());
        try {
            requireOne(mapper.create(row, actor()));
        } catch (DuplicateKeyException exception) {
            throw new ApiException(409, "缓存键已存在");
        }
        audit.platformSuccess("cache", row.getId(), "CREATE");
        return CacheDefinitionView.from(row);
    }

    @Transactional
    public CacheDefinitionView update(String id, UpdateCacheDefinitionCommand command) {
        requirePermission();
        var row = require(id);
        row.setVersion(command.version());
        fill(row, command.name(), command.cacheKey(), command.groupName(), command.refreshGroup(), command.scope(), command.description());
        try {
            requireOne(mapper.update(row, actor()));
        } catch (DuplicateKeyException exception) {
            throw new ApiException(409, "缓存键已存在");
        }
        row.setVersion(command.version() + 1);
        audit.platformSuccess("cache", row.getId(), "UPDATE");
        return CacheDefinitionView.from(row);
    }

    @Transactional
    public void delete(String id, int version) {
        requirePermission();
        require(id);
        requireOne(mapper.delete(id, version, actor()));
        audit.platformSuccess("cache", id, "DELETE");
    }

    @Transactional(propagation = Propagation.NEVER)
    public void refreshGroup(String refreshGroup, CacheScope scope) {
        requirePermission();
        var name = normalize(refreshGroup, 50, "刷新组名称");
        var rows = mapper.refreshGroup(name, scope.name());
        if (rows.isEmpty()) throw new ApiException(404, "刷新组不存在");
        var group = new CacheGroup("configured:" + scope.name() + ":" + name,
                rows.stream().map(row -> new CacheRegion(row.getCacheKey(), scope)).toList());
        groups.refresh(group);
        transactions.executeWithoutResult(status -> audit.platformSuccess("cache", auditId(scope, name), "REFRESH"));
    }

    private CacheDefinition require(String id) {
        var row = mapper.detail(id);
        if (row == null) throw new ApiException(404, "缓存定义不存在");
        return row;
    }

    private void fill(CacheDefinition row, String name, String cacheKey, String groupName, String refreshGroup,
                      CacheScope scope, String description) {
        row.setName(normalize(name, 100, "缓存名称"));
        row.setCacheKey(normalize(cacheKey, 100, "缓存键"));
        row.setGroupName(normalize(groupName, 50, "缓存分组名称"));
        row.setRefreshGroup(normalize(refreshGroup, 50, "刷新组名称"));
        row.setScope(scope.name());
        row.setDescription(description == null ? null : description.strip());
    }

    private static String normalize(String value, int max, String label) {
        var text = value == null ? "" : value.strip();
        if (text.isEmpty() || text.length() > max || text.startsWith("@")
                || text.indexOf('/') >= 0 || text.indexOf('\\') >= 0
                || text.chars().anyMatch(Character::isISOControl))
            throw new ApiException(400, label + "只能包含 1-" + max + " 个可见字符，且不能以 @ 开头");
        return text;
    }

    private void requirePermission() {
        var identity = ScopeContext.get();
        if (!authorization.hasPermission(identity.loginId(), "cache:user:refresh"))
            throw new ApiException(403, "没有缓存管理权限");
    }

    private static String actor() { return ScopeContext.get().loginId(); }

    private static String auditId(CacheScope scope, String name) {
        return "group-" + Integer.toUnsignedString((scope.name() + ":" + name).hashCode(), 36);
    }

    private static void requireOne(int count) {
        if (count != 1) throw new ApiException(409, "缓存定义已变更或删除，请刷新后重试");
    }
}
