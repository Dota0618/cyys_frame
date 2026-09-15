package com.cyys.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(classes = CyysApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"cyys.data-isolation.mode=multi", "cyys.data-isolation.single-scope-id="})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RoleIntegrationTest extends IsolationTestSupport {
    static final String ROLES = "/api/admin/roles";

    @BeforeEach
    void rolePermissions() {
        for (String permission : List.of("role:read", "role:create", "role:update", "role:delete", "role:grant")) {
            allow("role-a", permission, false);
        }
    }

    @AfterEach
    void cleanupManagedRoles() {
        List<String> ids = jdbc.queryForList("select id from sys_role where scope_id='scope-a' and code like 'G4%'", String.class);
        for (String id : ids) {
            jdbc.update("delete from sys_user_role where role_id=?", id);
            jdbc.update("delete from sys_role_menu where role_id=?", id);
            jdbc.update("delete from sys_role_org where role_id=?", id);
            jdbc.update("delete from sys_role where id=?", id);
        }
    }

    Reply roles(String method, String suffix, String token, String scope, Object body) throws Exception {
        return request(method, ROLES + suffix, token, scope, body == null ? null : JSON.writeValueAsString(body));
    }

    @Test
    void pageDetailAndOperationOptionsStayInCurrentUnit() throws Exception {
        jdbc.update("insert into sys_role(id,scope_id,code,name,sort) values('g1-role-second','scope-a','SECOND','第二角色',-1)");
        String token = token("alice");
        var page = roles("GET", "?size=20", token, "scope-a", null);
        assertThat(page.status()).as(page.raw()).isEqualTo(200);
        assertThat(page.body().at("/data/records/0/id").asText()).isEqualTo("g1-role-second");
        assertThat(page.raw()).doesNotContain("B_WRITER", "role-b");
        assertThat(roles("GET", "/role-b", token, "scope-a", null).status()).isEqualTo(404);

        var options = roles("GET", "/create-options", token, "scope-a", null);
        assertThat(options.status()).as(options.raw()).isEqualTo(200);
        assertThat(options.raw()).contains("permission-read", "g1-a-child").doesNotContain("permission-write", "g1-b-root");
        assertThat(roles("GET", "", token, null, null).status()).isEqualTo(403);
    }

    @Test
    void createEditAndDeleteKeepScopeRelationsAndVersionBoundary() throws Exception {
        platformUser();
        String token = token("alice");
        var create = Map.of(
                "code", "G4_EDITOR", "name", "测试编辑角色", "dataRange", 2, "status", 1, "sort", 3,
                "remark", "integration", "menuIds", List.of("permission-read"), "orgIds", List.of("g1-a-child"));
        var created = roles("POST", "", token, "scope-a", create);
        assertThat(created.status()).as(created.raw()).isEqualTo(200);
        String id = created.body().at("/data/id").asText();
        assertThat(created.body().at("/data/scopeId").asText()).isEqualTo("scope-a");
        assertThat(count("select count(*) from sys_role_org where role_id='" + id + "' and scope_id='scope-a' and org_id='g1-a-child'")).isOne();

        var edit = Map.of("id", id, "version", 0, "name", "测试编辑角色已修改", "dataRange", 1,
                "status", 1, "sort", 4, "remark", "updated", "menuIds", List.of("permission-read"), "orgIds", List.of());
        var edited = roles("PUT", "", token, "scope-a", edit);
        assertThat(edited.status()).as(edited.raw()).isEqualTo(200);
        assertThat(edited.body().at("/data/version").asInt()).isEqualTo(1);
        assertThat(count("select count(*) from sys_user_role where user_id='u-bob' and scope_id='scope-b'")).isOne();
        assertThat(count("select count(*) from sys_user_role where user_id='u-platform' and scope_id='GLOBAL'")).isOne();
        assertThat(roles("PUT", "", token, "scope-a", edit).status()).isEqualTo(409);

        membership("u-bob", "scope-a", false);
        grant("u-bob", id, "scope-a");
        assertThat(roles("DELETE", "/" + id + "?version=1", token, "scope-a", null).status()).isEqualTo(409);
        jdbc.update("delete from sys_user_role where user_id='u-bob' and role_id=? and scope_id='scope-a'", id);
        assertThat(roles("DELETE", "/" + id + "?version=1", token, "scope-a", null).status()).isEqualTo(200);
        assertThat(roles("GET", "/" + id, token, "scope-a", null).status()).isEqualTo(404);
    }

    @Test
    void saveRechecksGrantCeilingAndOperationPermission() throws Exception {
        String token = token("alice");
        var beyond = Map.of("code", "G4_BEYOND", "name", "越权角色", "dataRange", 1, "status", 1, "sort", 0,
                "menuIds", List.of("permission-write"), "orgIds", List.of());
        assertThat(roles("POST", "", token, "scope-a", beyond).status()).isEqualTo(403);
        var crossDepartment = Map.of("code", "G4_CROSS", "name", "跨单位角色", "dataRange", 2, "status", 1, "sort", 0,
                "menuIds", List.of("permission-read"), "orgIds", List.of("g1-b-root"));
        assertThat(roles("POST", "", token, "scope-a", crossDepartment).status()).isEqualTo(403);

        jdbc.update("delete from sys_role_menu where role_id='role-a' and menu_id in (select id from sys_menu where code='role:create')");
        assertThat(roles("GET", "/create-options", token, "scope-a", null).status()).isEqualTo(403);
        assertThat(roles("POST", "", token, "scope-a", beyond).status()).isEqualTo(403);
        assertThat(roles("GET", "", token, "scope-a", null).status()).isEqualTo(200);
    }
}
