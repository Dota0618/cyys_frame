package com.cyys.admin.cache.service;

import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.auth.mapper.DataAuthorizationMapper;
import com.cyys.admin.cache.dto.CacheActionView;
import com.cyys.common.cache.CacheGroupService;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/** 只登记已有缓存消费方的业务动作，不从数据库/请求执行类名、方法或 Redis 键。 */
@Service
@RequiredArgsConstructor
public class CacheManagementService {
    private static final CacheActionView USER_DISPLAY = new CacheActionView(BusinessCacheGroups.USERS.id(), "用户缓存",
            "当前单位", List.of("用户显示名称"), "用户新增、修改后自动失效；下次读取时重新加载。");
    private static final CacheActionView DICTIONARY = new CacheActionView(BusinessCacheGroups.DICTIONARIES.id(), "字典缓存",
            "系统共享", List.of("字典选项", "字典项名称与失效状态"), "字典分类、字典项增删改后自动失效；刷新影响全系统共享字典。");
    private final CacheGroupService groups;
    private final DataAuthorizationMapper authorization;
    private final DataAuditService audit;
    private final TransactionTemplate transactions;

    public List<CacheActionView> actions() {
        requirePermission();
        return canManageDictionary() ? List.of(USER_DISPLAY, DICTIONARY) : List.of(USER_DISPLAY);
    }

    @Transactional(propagation = Propagation.NEVER)
    public void refresh(String action) {
        requirePermission();
        var group = switch (action) {
            case "user-display" -> BusinessCacheGroups.USERS;
            case "dictionary" -> {
                if (!canManageDictionary()) throw new ApiException(403, "没有系统共享字典的管理权限");
                yield BusinessCacheGroups.DICTIONARIES;
            }
            default -> throw new ApiException(404, "缓存操作不存在");
        };
        // 先确认 Redis 操作完成，再记录成功；不能在提交后的失败回调之前记录成功审计。
        groups.refresh(group);
        transactions.executeWithoutResult(status -> {
            if (group.scope() == com.cyys.common.cache.CacheScope.SYSTEM) audit.platformSuccess("cache", group.id(), "REFRESH");
            else audit.success("cache", group.id(), "REFRESH");
        });
    }

    private boolean canManageDictionary() {
        return authorization.hasPermission(ScopeContext.get().loginId(), "dictionary:update");
    }

    private void requirePermission() {
        var identity = ScopeContext.get();
        if (!authorization.hasPermission(identity.loginId(), "cache:user:refresh")) {
            throw new ApiException(403, "没有当前单位的缓存管理权限");
        }
    }
}
