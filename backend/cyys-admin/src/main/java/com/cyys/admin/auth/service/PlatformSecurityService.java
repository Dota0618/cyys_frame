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

    private final DataAuthorizationMapper authorization;
    private final PlatformSecurityMapper mapper;

    public Set<String> authorize(String permission) {
        var permissions = permissionCeiling();
        if (!permissions.contains(permission)) {
            throw new ApiException(403, "没有此管理操作的权限");
        }
        return permissions;
    }

    /** 授权管理使用实时权限上限；普通接口的操作权限由 Controller 检查。 */
    public Set<String> permissionCeiling() {
        return Set.copyOf(authorization.permissions(ScopeContext.get().loginId()));
    }

    public void lockAdministratorGuard() {
        if (mapper.lockAdministratorPermission() == null) {
            throw new ApiException(409, "系统管理员关键权限不存在");
        }
    }

    public void requireUsableAdministrator() {
        if (mapper.usableAdministrators().isEmpty()) {
            throw new ApiException(409, "不能移除最后一个可用系统管理员");
        }
    }
    public void requireAllDataRange() {
        if (!authorization.hasAllDataRange(ScopeContext.get().loginId()))
            throw new ApiException(403, "此操作需要全系统数据管理范围");
    }

    public void requireDataRangeCeiling(int targetRange) {
        var actor = authorization.activeProfile(ScopeContext.get().loginId());
        if (actor == null || (targetRange == 0 && actor.getDataRange() != 0)
                || (targetRange == 6 && actor.getDataRange() != 0 && actor.getDataRange() != 6))
            throw new ApiException(403, "不能管理数据范围高于本人的用户");
    }
}
