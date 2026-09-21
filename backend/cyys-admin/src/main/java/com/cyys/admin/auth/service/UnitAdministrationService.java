package com.cyys.admin.auth.service;

import com.cyys.admin.auth.mapper.DataAuthorizationMapper;
import com.cyys.admin.user.mapper.SysUserRoleMapper;
import com.cyys.common.mybatis.DataPolicyRegistry;
import com.cyys.common.mybatis.DataTransactionGuard;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;

/** 角色与人员授权的统一管理边界；平台保持真实操作者，单位管理员须有本单位全部范围。 */
@Service
@RequiredArgsConstructor
public class UnitAdministrationService {
    private final DataAuthorizationMapper authorization;
    private final SysUserRoleMapper roles;

    public Set<String> authorize(String permission, boolean grantRoles) {
        var identity = ScopeContext.get();
        String scope = identity.requireScopeId();
        DataTransactionGuard.bind(identity, DataPolicyRegistry.View.CURRENT_SCOPE);
        var platform = Set.copyOf(roles.selectPermissionCodes(identity.loginId(), List.of("GLOBAL")));
        if (platform.containsAll(List.of("platform:scope:access", "platform:data:operate", permission))
                && (!grantRoles || platform.contains("role:grant"))) return platform;
        if (!authorization.hasPermission(identity.loginId(), scope, permission))
            throw new ApiException(403, "没有当前单位的授权管理权限");
        var unit = Set.copyOf(roles.selectUnitWidePermissionCodes(identity.loginId(), List.of(scope)));
        if (!unit.contains(permission) || (grantRoles && !unit.contains("role:grant")))
            throw new ApiException(403, "授权管理需要本单位全部数据范围及对应操作权限");
        return unit;
    }
}
