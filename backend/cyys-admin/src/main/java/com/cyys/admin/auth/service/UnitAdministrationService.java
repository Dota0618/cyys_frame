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

/** 用户授权须同时满足功能权限和可管理的机构范围。 */
@Service
@RequiredArgsConstructor
public class UnitAdministrationService {
    private final DataAuthorizationMapper authorization;
    private final SysUserRoleMapper roles;

    public Set<String> authorize(String permission, boolean grantRoles) {
        var identity = ScopeContext.get();
        String scope = identity.requireScopeId();
        DataTransactionGuard.bind(identity, DataPolicyRegistry.View.CURRENT_SCOPE);
        var permissions = Set.copyOf(roles.selectPermissionCodes(identity.loginId()));
        if (!permissions.contains(permission) || (grantRoles && !permissions.contains("role:grant")))
            throw new ApiException(403, "没有此操作的授权管理权限");
        if (!authorization.hasInstitutionWideRange(identity.loginId(), scope))
            throw new ApiException(403, "授权管理需要所选机构的完整数据范围");
        return permissions;
    }
    public void requireAssignableRange(com.cyys.admin.user.model.SysUserScope target, int range) {
        var actor = authorization.activeProfile(ScopeContext.get().loginId());
        if (actor == null) throw new ApiException(403, "操作者资料已失效");
        if (range != 0 && target.getScopeId() == null) throw new ApiException(400, "未归属机构的系统账号只能使用全系统数据范围");
        if (actor.getDataRange() == 0) return;
        if (target.getScopeId() == null || !authorization.hasInstitutionWideRange(actor.getUserId(), target.getScopeId())
                || target.getDataRange() == 0 || range == 0
                || ((target.getDataRange() == 6 || range == 6) && actor.getDataRange() != 6))
            throw new ApiException(403, "不能管理或授予超出本人数据范围的权限");
    }
}
