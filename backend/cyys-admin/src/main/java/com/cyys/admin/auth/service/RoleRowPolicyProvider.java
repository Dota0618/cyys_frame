package com.cyys.admin.auth.service;

import com.cyys.admin.auth.mapper.DataAuthorizationMapper;
import com.cyys.common.mybatis.DataAccess;
import com.cyys.common.mybatis.DataPolicyRegistry;
import com.cyys.common.mybatis.RowPolicyProvider;
import com.cyys.common.satoken.DataIsolationProperties;
import com.cyys.common.satoken.RequestIdentity;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import static com.cyys.common.mybatis.DataPolicyRegistry.denied;

@Service
public class RoleRowPolicyProvider implements RowPolicyProvider {
    private final ObjectProvider<DataAuthorizationMapper> lookups;
    private final DataIsolationProperties isolation;

    public RoleRowPolicyProvider(ObjectProvider<DataAuthorizationMapper> lookups, DataIsolationProperties isolation) {
        this.lookups = lookups;
        this.isolation = isolation;
    }

    @Override
    public void authorize(DataPolicyRegistry.Policy policy, RequestIdentity identity) {
        var lookup = lookups.getObject();
        String actor = identity.loginId();
        boolean all = policy.view() == DataAccess.View.AUTHORIZED_SCOPES;
        if (all) {
            if (!lookup.hasPermission(actor, "GLOBAL", "platform:data:read-all")
                    || !lookup.hasPermission(actor, "GLOBAL", policy.permission())) throw denied("没有此资源的跨单位读取权限");
        } else {
            String scope = identity.requireScopeId();
            if (isolation.mode() == DataIsolationProperties.Mode.SINGLE && !scope.equals(isolation.singleScopeId())) {
                throw denied("不能超出单单位部署范围");
            }
            boolean unitGrant = lookup.hasPermission(actor, scope, policy.permission());
            boolean platformGrant = lookup.hasPermission(actor, "GLOBAL", policy.permission())
                    && lookup.hasPermission(actor, "GLOBAL", "platform:scope:access")
                    && (lookup.hasPermission(actor, "GLOBAL", "platform:data:operate")
                        || (policy.operation() == DataAccess.Operation.READ
                            && lookup.hasPermission(actor, "GLOBAL", "platform:data:read-all")));
            if (!unitGrant && !platformGrant) throw denied("没有当前单位和操作的有效授权");
        }
    }

    @Override
    public String predicate(DataPolicyRegistry.Policy policy, RequestIdentity identity, String qualifier) {
        var resource = policy.resource();
        String owner = qualifier + "." + resource.scopeColumn();
        String actor = literal(identity.loginId());
        String permission = literal(policy.permission());
        String activeScope = "exists(select 1 from sys_scope pol_scope where pol_scope.id=" + owner
                + " and pol_scope.status=1 and pol_scope.deleted=0)";
        String platformResource = globalPermission(actor, permission);
        if (policy.view() == DataAccess.View.AUTHORIZED_SCOPES) {
            String ceiling = isolation.mode() == DataIsolationProperties.Mode.SINGLE
                    ? owner + "=" + literal(isolation.singleScopeId()) + " and " : "";
            return "(" + ceiling + activeScope + " and " + platformResource + " and "
                    + globalPermission(actor, literal("platform:data:read-all")) + ")";
        }
        String scope = literal(identity.requireScopeId());
        String department = qualifier + "." + resource.departmentColumn();
        String creator = qualifier + "." + resource.actorColumn();
        // 更新成员表时，MySQL 禁止在相关子查询中直接重读目标表；LIMIT 使当前操作者成员行物化。
        String membershipSource = resource.table().equals("sys_user_scope")
                ? "(select user_id,scope_id,org_id,status,deleted,expire_at from sys_user_scope where user_id=" + actor + " and scope_id=" + scope + " limit 1)"
                : "sys_user_scope";
        String descendants = resource.ancestorsColumn() != null
                ? "(pol_us.org_id=" + department + " or find_in_set(pol_us.org_id," + qualifier + "." + resource.ancestorsColumn() + ")>0)"
                : "exists(select 1 from sys_org pol_org where pol_org.id=" + department + " and pol_org.scope_id=" + owner
                    + " and pol_org.deleted=0 and (pol_us.org_id=pol_org.id or find_in_set(pol_us.org_id,pol_org.ancestors)>0))";
        String unit = "exists(select 1 from sys_user_role pol_ur "
                + "join sys_user pol_u on pol_u.id=pol_ur.user_id and pol_u.status=1 and pol_u.deleted=0 "
                + "join sys_role pol_r on pol_r.id=pol_ur.role_id and pol_r.scope_id=pol_ur.scope_id and pol_r.status=1 and pol_r.deleted=0 "
                + "join sys_role_menu pol_rm on pol_rm.role_id=pol_r.id and pol_rm.deleted=0 "
                + "join sys_menu pol_m on pol_m.id=pol_rm.menu_id and pol_m.status=1 and pol_m.deleted=0 and pol_m.type=3 and pol_m.platform_only=0 "
                + "join " + membershipSource + " pol_us on pol_us.user_id=pol_ur.user_id and pol_us.scope_id=pol_ur.scope_id "
                + "and pol_us.status=1 and pol_us.deleted=0 and (pol_us.expire_at is null or pol_us.expire_at>CURRENT_TIMESTAMP) "
                + "where pol_ur.deleted=0 and pol_ur.user_id=" + actor + " and pol_ur.scope_id=" + scope
                + " and pol_m.code=" + permission + " and (pol_r.data_range=1 "
                + "or (pol_r.data_range=2 and exists(select 1 from sys_role_org pol_ro where pol_ro.role_id=pol_r.id "
                + "and pol_ro.scope_id=" + owner + " and pol_ro.org_id=" + department + ")) "
                + "or (pol_r.data_range=3 and pol_us.org_id=" + department + ") "
                + "or (pol_r.data_range=4 and " + descendants + ") or (pol_r.data_range=5 and " + creator + "=" + actor + ")))";
        String platformAction = globalPermission(actor, literal("platform:data:operate"));
        if (policy.operation() == DataAccess.Operation.READ) {
            platformAction = "(" + platformAction + " or " + globalPermission(actor, literal("platform:data:read-all")) + ")";
        }
        return "(" + owner + "=" + scope + " and " + activeScope + " and (" + unit + " or ("
                + platformResource + " and " + platformAction + " and "
                + globalPermission(actor, literal("platform:scope:access")) + ")))";
    }

    private String globalPermission(String actor, String permission) {
        return "exists(select 1 from sys_user_role pol_gur "
                + "join sys_user pol_gu on pol_gu.id=pol_gur.user_id and pol_gu.status=1 and pol_gu.deleted=0 "
                + "join sys_role pol_gr on pol_gr.id=pol_gur.role_id and pol_gr.scope_id='GLOBAL' and pol_gr.status=1 and pol_gr.deleted=0 "
                + "join sys_role_menu pol_grm on pol_grm.role_id=pol_gr.id and pol_grm.deleted=0 "
                + "join sys_menu pol_gm on pol_gm.id=pol_grm.menu_id and pol_gm.type=3 and pol_gm.status=1 and pol_gm.deleted=0 "
                + "where pol_gur.deleted=0 and pol_gur.scope_id='GLOBAL' and pol_gur.user_id=" + actor + " and pol_gm.code=" + permission + ")";
    }

    private static String literal(String value) { return "'" + value.replace("'", "''").replace("\\", "\\\\") + "'"; }
}
