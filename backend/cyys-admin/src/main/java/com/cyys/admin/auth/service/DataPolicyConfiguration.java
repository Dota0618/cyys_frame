package com.cyys.admin.auth.service;

import com.cyys.common.mybatis.DataPolicyRegistry;
import com.cyys.common.mybatis.DataResource;
import org.apache.ibatis.mapping.SqlCommandType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@Configuration(proxyBeanMethods = false)
public class DataPolicyConfiguration {
    @Bean
    DataPolicyRegistry dataPolicyRegistry() {
        var lookups = new LinkedHashMap<String, SqlCommandType>();
        String base = "com.cyys.admin.";
        for (String method : List.of("user.mapper.SysUserMapper.selectById", "user.mapper.SysUserMapper.selectForLogin",
                "user.mapper.SysUserScopeMapper.selectAvailableScopes", "user.mapper.SysUserRoleMapper.selectRoleCodes",
                "user.mapper.SysUserRoleMapper.selectUnitWidePermissionCodes",
                "user.mapper.SysUserRoleMapper.selectPermissionCodes", "scope.mapper.SysScopeMapper.selectEnabledById",
                "scope.mapper.SysScopeMapper.selectEnabledScopes", "auth.mapper.DataAuthorizationMapper.hasPermission",
                "department.mapper.SysOrgMapper.hasActiveReferences", "role.mapper.RoleGrantMapper.lockMembership",
                "role.mapper.RoleGrantMapper.lockUnitRoles", "role.mapper.RoleGrantMapper.permissions",
                "role.mapper.RoleGrantMapper.activeUnitRoles",
                "user.mapper.UserDisplayMapper.visibleMembers", "user.mapper.UserDisplayMapper.labels")) {
            lookups.put(base + method, SqlCommandType.SELECT);
        }
        for (String method : List.of("page", "detail", "lock", "menus", "departments", "menuIds", "orgIds",
                "lockMenus", "lockDepartments", "hasUserGrant")) {
            lookups.put(base + "role.mapper.SysRoleMapper." + method, SqlCommandType.SELECT);
        }
        for (String method : List.of("create", "addMenu", "addDepartment")) {
            lookups.put(base + "role.mapper.SysRoleMapper." + method, SqlCommandType.INSERT);
        }
        for (String method : List.of("updateRole", "softDelete")) {
            lookups.put(base + "role.mapper.SysRoleMapper." + method, SqlCommandType.UPDATE);
        }
        for (String method : List.of("removeMenus", "removeDepartments")) {
            lookups.put(base + "role.mapper.SysRoleMapper." + method, SqlCommandType.DELETE);
        }
        for (String method : List.of("accountNames", "roleIds")) {
            lookups.put(base + "user.mapper.MemberAccountMapper." + method, SqlCommandType.SELECT);
        }
        lookups.put(base + "user.mapper.MemberAccountMapper.createAccount", SqlCommandType.INSERT);
        lookups.put(base + "user.mapper.SysUserMapper.recordPasswordFailure", SqlCommandType.UPDATE);
        lookups.put(base + "user.mapper.SysUserMapper.recordLoginSuccess", SqlCommandType.UPDATE);
        lookups.put(base + "audit.mapper.DataAuditMapper.insertSuccess", SqlCommandType.INSERT);
        lookups.put(base + "role.mapper.RoleGrantMapper.removeUnitGrants", SqlCommandType.DELETE);
        lookups.put(base + "role.mapper.RoleGrantMapper.addUnitGrant", SqlCommandType.INSERT);
        return new DataPolicyRegistry(List.of(
                new DataResource("department", "sys_org", "scope_id", "id", "ancestors", "created_by", "version", Set.of("parent_id", "ancestors")),
                new DataResource("user", "sys_user_scope", "scope_id", "org_id", null, "user_id", "version", Set.of("user_id", "member_role", "default_flag", "expire_at"))), lookups);
    }
}
