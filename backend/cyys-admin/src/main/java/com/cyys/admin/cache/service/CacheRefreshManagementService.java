package com.cyys.admin.cache.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.auth.mapper.DataAuthorizationMapper;
import com.cyys.admin.cache.dto.CacheRefreshItemView;
import com.cyys.admin.cache.dto.CreateCacheRefreshItemCommand;
import com.cyys.admin.cache.dto.UpdateCacheRefreshItemCommand;
import com.cyys.admin.cache.mapper.CacheRefreshItemMapper;
import com.cyys.admin.cache.model.CacheRefreshItem;
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
public class CacheRefreshManagementService {
    private final CacheRefreshItemMapper mapper;
    private final CacheGroupService groups;
    private final DataAuthorizationMapper authorization;
    private final DataAuditService audit;
    private final TransactionTemplate transactions;

    public List<CacheRefreshItemView> list() {
        requirePermission();
        return mapper.list().stream().map(CacheRefreshItemView::from).toList();
    }

    @Transactional
    public CacheRefreshItemView create(CreateCacheRefreshItemCommand command) {
        requirePermission();
        var row = new CacheRefreshItem();
        row.setId(IdWorker.getIdStr());
        row.setName(normalizeName(command.name()));
        try {
            requireOne(mapper.create(row, actor()));
        } catch (DuplicateKeyException exception) {
            throw new ApiException(409, "刷新名称已存在");
        }
        audit.platformSuccess("cache", row.getId(), "CREATE");
        return CacheRefreshItemView.from(row);
    }

    @Transactional
    public CacheRefreshItemView update(String id, UpdateCacheRefreshItemCommand command) {
        requirePermission();
        var row = require(id);
        row.setName(normalizeName(command.name()));
        row.setVersion(command.version());
        try {
            requireOne(mapper.update(row, actor()));
        } catch (DuplicateKeyException exception) {
            throw new ApiException(409, "刷新名称已存在");
        }
        row.setVersion(command.version() + 1);
        audit.platformSuccess("cache", row.getId(), "UPDATE");
        return CacheRefreshItemView.from(row);
    }

    @Transactional
    public void delete(String id, int version) {
        requirePermission();
        require(id);
        requireOne(mapper.delete(id, version, actor()));
        audit.platformSuccess("cache", id, "DELETE");
    }

    @Transactional(propagation = Propagation.NEVER)
    public void refresh(String id) {
        requirePermission();
        var item = require(id);
        var group = new CacheGroup("configured:" + item.getId(),
                List.of(new CacheRegion(item.getName(), CacheScope.SYSTEM)));
        groups.refresh(group);
        transactions.executeWithoutResult(status -> audit.platformSuccess("cache", item.getId(), "REFRESH"));
    }

    private CacheRefreshItem require(String id) {
        var item = mapper.detail(id);
        if (item == null) throw new ApiException(404, "刷新配置不存在");
        return item;
    }

    private void requirePermission() {
        var identity = ScopeContext.get();
        if (!authorization.hasPermission(identity.loginId(), "cache:user:refresh"))
            throw new ApiException(403, "没有缓存管理权限");
    }

    private static String normalizeName(String value) {
        var name = value == null ? "" : value.strip();
        if (name.isEmpty() || name.length() > 50 || name.startsWith("@")
                || name.indexOf('/') >= 0 || name.indexOf('\\') >= 0
                || name.chars().anyMatch(Character::isISOControl))
            throw new ApiException(400, "刷新名称只能包含 1-50 个可见字符，且不能以 @ 开头");
        return name;
    }

    private static String actor() { return ScopeContext.get().loginId(); }

    private static void requireOne(int count) {
        if (count != 1) throw new ApiException(409, "刷新配置已变更或删除，请刷新后重试");
    }
}
