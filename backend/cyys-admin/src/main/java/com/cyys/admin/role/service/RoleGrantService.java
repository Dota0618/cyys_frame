package com.cyys.admin.role.service;

import com.cyys.admin.auth.service.UnitAdministrationService;
import com.cyys.admin.role.dto.RoleOption;
import com.cyys.admin.role.mapper.RoleGrantMapper;
import com.cyys.common.mybatis.DataPolicyRegistry;
import com.cyys.common.mybatis.DataTransactionGuard;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleGrantService {
    private final RoleGrantMapper mapper;
    private final UnitAdministrationService administration;
    private final RoleService roles;
    private final com.cyys.admin.auth.service.PlatformSecurityService security;

    @Transactional(readOnly = true)
    public List<RoleOption> grantableRoleOptions() {
        var identity = ScopeContext.get();
        identity.requireScopeId();
        DataTransactionGuard.bind(identity, DataPolicyRegistry.View.CURRENT_SCOPE);
        Set<String> ceiling = administration.authorize("role:grant", true);
        List<RoleOption> candidates = mapper.activeRoles();
        if (candidates.isEmpty()) return candidates;
        List<String> roleIds = candidates.stream().map(RoleOption::value).toList();
        Set<String> rejected = new HashSet<>();
        for (var permission : mapper.permissions(roleIds)) {
            if (!ceiling.contains(permission.code())) rejected.add(permission.roleId());
        }
        var result = new ArrayList<RoleOption>();
        for (var role : candidates) {
            if (!rejected.contains(role.value())) result.add(role);
        }
        return List.copyOf(result);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void replaceRoles(String userId,
                                 List<String> roleIds) {
        var identity = ScopeContext.get();
        String scope = identity.requireScopeId();
        DataTransactionGuard.bind(identity, DataPolicyRegistry.View.CURRENT_SCOPE);
        Set<String> ceiling = administration.authorize("role:grant", true);
        if (Set.copyOf(roleIds).size() != roleIds.size()) throw new ApiException(400, "角色不能重复");
        security.lockAdministratorGuard();
        var target = mapper.lockProfile(userId);
        if (target == null) throw new ApiException(403, "目标用户不可操作");
        ceiling = administration.authorize("role:grant", true);
        security.requireDataRangeCeiling(target.getDataRange());
        if (target.getScopeId() == null || target.getDataRange() == 0) security.requireAllDataRange();
        if (target.getScopeId() != null && !scope.equals(target.getScopeId())) throw new ApiException(403, "目标用户不属于所选机构");
        roles.assignAuthorized(userId, roleIds, ceiling);
    }
}
