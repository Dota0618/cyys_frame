package com.cyys.admin.auth.service;

import com.cyys.admin.auth.mapper.DataAuthorizationMapper;
import com.cyys.admin.auth.mapper.PlatformSecurityMapper;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PlatformSecurityService {
    public static final String GLOBAL = "GLOBAL";
    public static final String PLATFORM_ACCESS = "platform:scope:access";

    private final DataAuthorizationMapper authorization;
    private final PlatformSecurityMapper mapper;

    public Set<String> authorize(String permission) {
        var identity = ScopeContext.get();
        if (!authorization.hasPermission(identity.loginId(), GLOBAL, PLATFORM_ACCESS)
                || !authorization.hasPermission(identity.loginId(), GLOBAL, permission)) {
            throw new ApiException(403, "没有平台控制权限");
        }
        return Set.copyOf(identity.platformPermissions());
    }

    public void lockAdministratorGuard() {
        if (mapper.lockAdministratorPermission() == null) {
            throw new ApiException(409, "平台管理员关键权限不存在");
        }
    }

    public void requireUsableAdministrator() {
        if (mapper.usableAdministrators().isEmpty()) {
            throw new ApiException(409, "不能移除最后一个可用平台管理员");
        }
    }
}
