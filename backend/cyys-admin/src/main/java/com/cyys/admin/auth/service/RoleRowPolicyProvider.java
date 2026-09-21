package com.cyys.admin.auth.service;

import com.cyys.admin.auth.mapper.DataAuthorizationMapper;
import com.cyys.common.mybatis.DataPolicyRegistry;
import com.cyys.common.mybatis.RowPolicyProvider;
import com.cyys.common.satoken.RequestIdentity;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import static com.cyys.common.mybatis.DataPolicyRegistry.denied;

/** 仅计算用户数据范围；功能权限由入口的 Sa-Token 注解检查。 */
@Service
public class RoleRowPolicyProvider implements RowPolicyProvider {
    private final ObjectProvider<DataAuthorizationMapper> lookups;

    public RoleRowPolicyProvider(ObjectProvider<DataAuthorizationMapper> lookups) {
        this.lookups = lookups;
    }

    @Override
    public void authorize(DataPolicyRegistry.Policy policy, RequestIdentity identity) {
        var lookup = lookups.getObject();
        if (lookup.activeProfile(identity.loginId()) == null) throw denied("没有有效的用户资料");
        if (identity.scopeId() != null && !lookup.hasInstitutionAccess(identity.loginId(), identity.scopeId()))
            throw denied("所选机构已不在当前数据范围内");
        if (policy.view() == DataPolicyRegistry.View.CURRENT_SCOPE) identity.requireScopeId();
    }

    /** 用于机构取号等没有业务行的操作；机构资格仍由当前用户数据范围核验。 */
    public void authorizeCurrentScope(String permission, RequestIdentity identity) {
        var lookup = lookups.getObject();
        if (!lookup.hasPermission(identity.loginId(), permission)
                || !lookup.hasInstitutionAccess(identity.loginId(), identity.requireScopeId()))
            throw denied("没有所选机构和操作的有效授权");
    }

    @Override
    public String predicate(DataPolicyRegistry.Policy policy, RequestIdentity identity, String qualifier) {
        var resource = policy.resource();
        String owner = qualifier + "." + resource.scopeColumn();
        String department = qualifier + "." + resource.departmentColumn();
        String creator = qualifier + "." + resource.actorColumn();
        String actor = literal(identity.loginId());
        boolean userProfile = resource.table().equals("sys_user_scope");
        // 更新用户资料时物化操作者资料，避免 MySQL 同表更新子查询限制。
        String profileSource = userProfile
                ? "(select id,user_id,scope_id,org_id,data_range,status,deleted from sys_user_scope where user_id=" + actor + " limit 1)"
                : "sys_user_scope";
        String activeOwner = "exists(select 1 from sys_scope pol_scope where pol_scope.id=" + owner
                + " and pol_scope.status=1 and pol_scope.deleted=0)";
        if (userProfile) activeOwner = "(" + owner + " is null or " + activeOwner + ")";
        String filter = identity.scopeId() == null ? "" : "(" + owner + "=" + literal(identity.scopeId())
                + (userProfile ? " or " + owner + " is null" : "") + ") and ";
        String departmentDescendants = resource.ancestorsColumn() != null
                ? "(p.org_id=" + department + " or find_in_set(p.org_id," + qualifier + "." + resource.ancestorsColumn() + ")>0)"
                : "exists(select 1 from sys_org pol_org where pol_org.id=" + department + " and pol_org.scope_id=" + owner
                    + " and pol_org.deleted=0 and (p.org_id=pol_org.id or find_in_set(p.org_id,pol_org.ancestors)>0))";
        String sameInstitution = owner + "=p.scope_id and (p.data_range=1"
                + " or (p.data_range=2 and exists(select 1 from sys_user_scope_org pol_custom where pol_custom.member_id=p.id"
                + " and pol_custom.scope_id=" + owner + " and pol_custom.org_id=" + department + "))"
                + " or (p.data_range=3 and p.org_id=" + department + ")"
                + " or (p.data_range=4 and " + departmentDescendants + ")"
                + " or (p.data_range=5 and " + creator + "=" + actor + "))";
        String institutions = "p.data_range=6 and exists(select 1 from sys_scope pol_child where pol_child.id=" + owner
                + " and (pol_child.id=p.scope_id or find_in_set(p.scope_id,pol_child.ancestors)>0))";
        String range = "exists(select 1 from " + profileSource + " p where p.user_id=" + actor
                + " and p.status=1 and p.deleted=0 and (p.data_range=0 or ("
                + "exists(select 1 from sys_scope pol_home where pol_home.id=p.scope_id and pol_home.status=1 and pol_home.deleted=0)"
                + " and ((" + sameInstitution + ") or (" + institutions + ")))))";
        return "(" + filter + activeOwner + " and " + range + ")";
    }

    private static String literal(String value) { return "'" + value.replace("'", "''").replace("\\", "\\\\") + "'"; }
}
