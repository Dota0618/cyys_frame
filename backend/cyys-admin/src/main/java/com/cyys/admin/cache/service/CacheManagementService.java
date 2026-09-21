package com.cyys.admin.cache.service;

import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.auth.mapper.DataAuthorizationMapper;
import com.cyys.admin.cache.dto.CacheActionView;
import com.cyys.admin.user.service.UserDisplayService;
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
    private static final CacheActionView USER_DISPLAY = new CacheActionView("user-display", "用户显示名称",
            "更新当前单位的用户显示名称，后续读取时重新加载。");
    private final UserDisplayService users;
    private final DataAuthorizationMapper authorization;
    private final DataAuditService audit;
    private final TransactionTemplate transactions;

    public List<CacheActionView> actions() {
        requirePermission();
        return List.of(USER_DISPLAY);
    }

    @Transactional(propagation = Propagation.NEVER)
    public void refresh(String action) {
        requirePermission();
        if (!USER_DISPLAY.id().equals(action)) throw new ApiException(404, "缓存操作不存在");
        // 先确认 Redis 操作完成，再记录成功；不能在提交后的失败回调之前记录成功审计。
        users.refreshCurrentUnit();
        transactions.executeWithoutResult(status -> audit.success("cache", USER_DISPLAY.id(), "REFRESH"));
    }

    private void requirePermission() {
        var identity = ScopeContext.get();
        if (!authorization.hasPermission(identity.loginId(), "cache:user:refresh")) {
            throw new ApiException(403, "没有当前单位的缓存管理权限");
        }
    }
}
