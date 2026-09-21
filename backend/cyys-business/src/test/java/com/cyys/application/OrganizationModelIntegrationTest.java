package com.cyys.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

/** 只允许在 MySqlTestDatabaseGuard 确认的空验收库运行，不连接含业务数据的开发库。 */
@ActiveProfiles("test")
@SpringBootTest(classes=CyysApplication.class, webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class OrganizationModelIntegrationTest extends HttpTestSupport {
    private final List<String> createdRoles = new ArrayList<>();
    private String extraInstitution;

    @BeforeEach void organizationFixtures() {
        jdbc.update("update sys_scope set parent_id='scope-a',ancestors='scope-a' where id='scope-b'");
        jdbc.update("update sys_user_scope set data_range=6,display_name='上级管理员' where user_id='u-alice'");
        jdbc.update("update sys_user_scope set data_range=1,display_name='下级用户' where user_id='u-bob'");
        user("u-platform", "system-admin");
        jdbc.update("insert into sys_user_scope(id,user_id,scope_id,display_name,data_range) values('g1-system-profile','u-platform',NULL,'系统管理员',0)");
        role("role-platform", "GLOBAL", "SYSTEM_ADMIN");
        grant("u-platform", "role-platform", "GLOBAL");
        for (String permission : List.of("role:read", "role:create", "role:update", "role:delete", "role:grant",
                "user:read", "user:update", "user:data-permission", "department:read", "file:read", "file:update",
                "org-notice:read", "org-notice:create", "org-notice:update", "dictionary:read",
                "platform:scope:read", "platform:scope:create", "platform:scope:update", "menu:read", "menu:update")) {
            allow("role-platform", permission);
        }
        for (String permission : List.of("user:read", "user:update", "department:read", "file:read", "file:update",
                "org-notice:read", "org-notice:create", "org-notice:update", "dictionary:read", "role:read",
                "role:grant", "user:data-permission", "platform:account:reset-password")) {
            allow("role-a", permission);
        }
        allow("role-b", "file:read"); allow("role-b", "user:read");
        jdbc.update("insert into sys_org(id,scope_id,name,code,ancestors,created_by) values('g1-b-department','scope-b','下级部门','QA_DEPT','','u-bob')");
        jdbc.update("update sys_user_scope set org_id='g1-b-department' where user_id='u-bob'");
        jdbc.update("insert into sys_file_entry(id,scope_id,org_id,name,storage_profile,object_key,state,byte_size,created_by,created_at,updated_by,updated_at) "
                + "values('g1-org-file','scope-b','g1-b-department','验收文件','local','organization-acceptance-object','READY',0,'u-bob',CURRENT_TIMESTAMP,'u-bob',CURRENT_TIMESTAMP)");
    }

    @AfterEach void removeOrganizationFixtures() {
        jdbc.update("delete from sys_file_entry where id='g1-org-file' and object_key='organization-acceptance-object'");
        jdbc.update("delete from sys_org_notice where scope_id='scope-b' and created_by='u-alice'");
        jdbc.update("delete from sys_data_audit where actor_id='u-platform' or actor_id='u-alice'");
        for (String id : createdRoles) {
            jdbc.update("delete from sys_user_role where role_id=?", id);
            jdbc.update("delete from sys_role_menu where role_id=?", id);
            jdbc.update("delete from sys_role where id=? and code='ORG_QA_ROLE'", id);
        }
        jdbc.update("update sys_scope set parent_id='0',ancestors='' where id in ('scope-a','scope-b')");
        if (extraInstitution != null) jdbc.update("delete from sys_scope where id=? and remark='CYYS organization acceptance fixture'", extraInstitution);
    }

    @Test void unifiedRolesHierarchyAndUserRangeWorkAcrossRealRequests() throws Exception {
        assertThat(count("select count(*) from information_schema.columns where table_schema=database() "
                + "and table_name in ('sys_role','sys_user_role') and column_name='scope_id'")).isZero();
        assertThat(count("select count(*) from information_schema.columns where table_schema=database() "
                + "and table_name='sys_menu' and column_name='platform_only'")).isZero();
        assertThatThrownBy(() -> jdbc.update("insert into sys_user_scope(id,user_id,scope_id) values('g1-duplicate-profile','u-alice','scope-b')"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

        String admin = token("system-admin");
        String alice = token("alice");
        String bob = token("bob");
        var inParent = ok("GET", "/api/auth/me", alice, "scope-a", null).body().path("data");
        var inChild = ok("GET", "/api/auth/me", alice, "scope-b", null).body().path("data");
        assertThat(inChild.path("roles")).isEqualTo(inParent.path("roles"));
        assertThat(inChild.path("permissions")).isEqualTo(inParent.path("permissions"));
        assertThat(inChild.path("scopes").size()).isEqualTo(2);
        assertThat(inChild.has("unitPermissions")).isFalse();
        assertThat(request("GET", "/api/admin/users", bob, "scope-a", null).status()).isEqualTo(403);
        assertThat(ok("GET", "/api/admin/users", alice, "scope-b", null).raw()).contains("下级用户").doesNotContain("系统管理员");
        assertThat(ok("GET", "/api/admin/users", admin, "scope-b", null).raw()).contains("系统管理员");
        assertThat(ok("GET", "/api/admin/departments", alice, "scope-b", null).raw()).contains("下级部门");
        assertThat(ok("GET", "/api/admin/files", alice, "scope-b", null).raw()).contains("验收文件");
        ok("PUT", "/api/admin/files/g1-org-file", alice, "scope-b", Map.of("version",0,"name","上级修改的文件"));
        assertThat(request("PUT", "/api/admin/files/g1-org-file", alice, "scope-a", JSON.writeValueAsString(Map.of("version",1,"name","越界修改"))).status()).isEqualTo(409);
        ok("GET", "/api/admin/platform/dictionaries", alice, "scope-b", null);

        var notice = ok("POST", "/api/admin/org-notices", alice, "scope-b", Map.of(
                "departmentId","g1-b-department","title","机构通知验收","content","验收内容","attachments",List.of()));
        assertThat(notice.raw()).contains("机构通知验收");
        assertThat(ok("GET", "/api/admin/org-notices", alice, "scope-b", null).raw()).contains("机构通知验收");

        String readPermission = jdbc.queryForObject("select id from sys_menu where code='user:read'",String.class);
        var created = ok("POST", "/api/admin/roles", admin, null, Map.of("code","ORG_QA_ROLE","name","统一角色验收",
                "status",1,"sort",0,"menuIds",List.of(readPermission)));
        String role = created.body().at("/data/id").asText(); createdRoles.add(role);
        var parentRoles = ok("GET", "/api/admin/roles", alice, "scope-a", null).body().at("/data/records");
        var childRoles = ok("GET", "/api/admin/roles", alice, "scope-b", null).body().at("/data/records");
        assertThat(childRoles).isEqualTo(parentRoles);
        assertThat(parentRoles.toString()).contains("统一角色验收");

        // 功能授权不能提升数据范围，也不能用机构管理权限改动系统管理员。
        assertThat(request("PUT", "/api/admin/users/u-bob-scope-b/data-permission", alice, "scope-b",
                JSON.writeValueAsString(Map.of("version",0,"dataRange",0,"orgIds",List.of()))).status()).isEqualTo(403);
        assertThat(request("PUT", "/api/admin/users/g1-system-profile", alice, "scope-b",
                JSON.writeValueAsString(Map.of("version",0,"displayName","越权修改","status",1))).status()).isIn(403,409);
        assertThat(request("POST", "/api/admin/platform/accounts/u-platform/password", alice, "scope-b",
                JSON.writeValueAsString(Map.of("newPassword","unreachable-password-2026"))).status()).isEqualTo(403);

        // 数据范围收回立即生效，不保留先前已选机构或旧登录时的权限快照。
        ok("PUT", "/api/admin/users/u-alice-scope-a/data-permission", admin, "scope-a",
                Map.of("version",0,"dataRange",1,"orgIds",List.of()));
        assertThat(request("GET", "/api/admin/files", alice, "scope-b", null).status()).isEqualTo(403);
        assertThat(ok("GET", "/api/auth/me", alice, "scope-a", null).body().at("/data/scopes").size()).isOne();

        assertThat(request("PUT", "/api/admin/users/g1-system-profile", admin, "scope-a",
                JSON.writeValueAsString(Map.of("version",0,"displayName","系统管理员","status",0))).status()).isEqualTo(409);
        assertThat(count("select status from sys_user_scope where id='g1-system-profile'")).isEqualTo(1);

        var institution = ok("POST", "/api/admin/platform/scopes", admin, null, Map.of("code","ORG_QA_EXTRA",
                "name","其他分支","parentId","0","remark","CYYS organization acceptance fixture"));
        extraInstitution = institution.body().at("/data/id").asText();
        assertThat(ok("GET", "/api/admin/platform/scopes/tree", admin, null, null).raw()).contains("其他分支");
        assertThat(request("PUT", "/api/admin/platform/scopes/scope-a", admin, null,
                JSON.writeValueAsString(Map.of("version",0,"name","父机构","enabled",true,"sort",0,"parentId","scope-b"))).status()).isEqualTo(400);
        ok("PUT", "/api/admin/platform/scopes/scope-b", admin, null,
                Map.of("version",0,"name","移动后的下级","enabled",true,"sort",0,"parentId",extraInstitution,"remark","CYYS automated test fixture"));
        assertThat(jdbc.queryForObject("select ancestors from sys_scope where id='scope-b'",String.class)).isEqualTo(extraInstitution);
        ok("PUT", "/api/admin/platform/scopes/scope-b", admin, "scope-b",
                Map.of("version",1,"name","移动后的下级","enabled",false,"sort",0,"parentId",extraInstitution,"remark","CYYS automated test fixture"));
        assertThat(request("GET", "/api/auth/me", admin, "scope-b", null).status()).isEqualTo(403);
        assertThat(ok("GET", "/api/auth/me", admin, null, null).body().at("/data/scopes").toString()).doesNotContain("移动后的下级");
        assertThat(request("GET", "/api/admin/platform/roles", admin, "scope-a", null).status()).isEqualTo(404);
        assertThat(request("GET", "/api/admin/generator/tables", admin, "scope-a", null).status()).isEqualTo(404);
    }

    private void allow(String role, String code) {
        var existing=jdbc.queryForList("select id from sys_menu where code=?",String.class,code);
        if(existing.isEmpty()) permission(role,"g1-"+IdWorker.getIdStr(),code,false);
        else jdbc.update("insert into sys_role_menu(id,role_id,menu_id) values(?,?,?)",IdWorker.getIdStr(),role,existing.getFirst());
    }
    private Reply ok(String method,String path,String token,String scope,Object body) throws Exception {
        Reply reply=request(method,path,token,scope,body==null?null:JSON.writeValueAsString(body));
        assertThat(reply.status()).as(method+" "+path+": "+reply.raw()).isEqualTo(200);
        return reply;
    }
    private int count(String sql) { return jdbc.queryForObject(sql,Integer.class); }
}
