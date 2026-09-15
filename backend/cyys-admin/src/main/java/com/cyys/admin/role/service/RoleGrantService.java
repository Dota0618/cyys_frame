package com.cyys.admin.role.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.auth.mapper.DataAuthorizationMapper;
import com.cyys.admin.role.mapper.RoleGrantMapper;
import com.cyys.admin.user.mapper.SysUserRoleMapper;
import com.cyys.common.mybatis.DataAccess;
import com.cyys.common.mybatis.DataTransactionGuard;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.util.List;
import java.util.Set;

@Service
@Validated
@RequiredArgsConstructor
public class RoleGrantService {
    private final RoleGrantMapper mapper;
    private final DataAuthorizationMapper authorization;
    private final SysUserRoleMapper roles;
    private final DataAuditService audit;

    @Transactional(readOnly = true)
    public List<RoleGrantMapper.RoleOption> grantableRoleOptions() {
        var identity = ScopeContext.get();
        String scope = identity.requireScopeId();
        DataTransactionGuard.bind(identity, DataAccess.View.CURRENT_SCOPE);
        if (!authorization.hasPermission(identity.loginId(), scope, "role:grant")) {
            throw new ApiException(403, "没有本单位角色授予权限");
        }
        Set<String> ceiling = Set.copyOf(roles.selectUnitWidePermissionCodes(identity.loginId(), List.of(scope)));
        if (!ceiling.contains("role:grant")) throw new ApiException(403, "局部数据范围的角色委派尚未开放");
        List<RoleGrantMapper.RoleOption> candidates = mapper.activeUnitRoles(scope);
        if (candidates.isEmpty()) return candidates;
        Set<String> rejected = mapper.permissions(candidates.stream().map(RoleGrantMapper.RoleOption::value).toList()).stream()
                .filter(permission -> permission.platformOnly() || !ceiling.contains(permission.code()))
                .map(RoleGrantMapper.GrantPermission::roleId).collect(java.util.stream.Collectors.toSet());
        return candidates.stream().filter(role -> !rejected.contains(role.value())).toList();
    }

    @Transactional
    public void replaceUnitRoles(@NotBlank @Size(max=32) String userId,
                                 @NotNull @Size(max=100) List<@NotBlank String> roleIds) {
        var identity = ScopeContext.get();
        String scope = identity.requireScopeId();
        DataTransactionGuard.bind(identity, DataAccess.View.CURRENT_SCOPE);
        if (!authorization.hasPermission(identity.loginId(), scope, "role:grant")) {
            throw new ApiException(403, "没有本单位角色授予权限");
        }
        Set<String> ceiling = Set.copyOf(roles.selectUnitWidePermissionCodes(identity.loginId(), List.of(scope)));
        if (!ceiling.contains("role:grant")) throw new ApiException(403, "局部数据范围的角色委派尚未开放");
        if (Set.copyOf(roleIds).size() != roleIds.size()) throw new ApiException(400, "角色不能重复");
        if (mapper.lockMembership(userId, scope) == null) throw new ApiException(403, "目标成员不可操作");
        if (!roleIds.isEmpty()) {
            if (mapper.lockUnitRoles(roleIds, scope).size() != roleIds.size()) throw new ApiException(403, "不能授予平台、其他单位或无效角色");
            if (mapper.permissions(roleIds).stream().anyMatch(permission -> permission.platformOnly() || !ceiling.contains(permission.code()))) {
                throw new ApiException(403, "角色权限超出当前操作者的授予上限");
            }
        }
        mapper.removeUnitGrants(userId, scope);
        for (String role : roleIds) {
            if (mapper.addUnitGrant(IdWorker.getIdStr(), userId, role, scope, identity.loginId()) != 1) {
                throw new ApiException(409, "角色授予冲突");
            }
        }
        audit.success("role_grant", userId, "REPLACE");
    }
}
