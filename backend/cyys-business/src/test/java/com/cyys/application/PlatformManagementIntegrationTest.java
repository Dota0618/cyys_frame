package com.cyys.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(classes = CyysApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"cyys.data-isolation.mode=multi", "cyys.data-isolation.single-scope-id="})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PlatformManagementIntegrationTest extends HttpTestSupport {
    static final String MENUS = "/api/admin/platform/menus";
    static final String ROLES = "/api/admin/platform/roles";
    static final List<String> MANAGEMENT = List.of("menu:read", "menu:create", "menu:update", "menu:delete",
            "platform:role:read", "platform:role:create", "platform:role:update", "platform:role:delete",
            "platform:role:grant");

    @BeforeEach
    void platformPermissions() {
        platformUser();
        jdbc.update("delete from sys_role_menu where id='platform-read'");
        for (String permission : MANAGEMENT) allowPlatform(permission);
        jdbc.update("insert into sys_menu(id,code,name,type,platform_only) values('g1-guard-menu','g1:guard-menu','平台管理',2,1)");
        jdbc.update("update sys_menu set parent_id='g1-guard-menu' where id='platform-access'");
    }

    @AfterEach
    void cleanupPlatformRecords() {
        jdbc.update("delete from sys_data_audit where scope_id='GLOBAL'");
        jdbc.update("delete from sys_user_role where role_id in (select id from sys_role where code like 'G4P_%')");
        jdbc.update("delete from sys_role_menu where role_id in (select id from sys_role where code like 'G4P_%')");
        jdbc.update("delete from sys_role where code like 'G4P_%'");
        jdbc.update("delete from sys_role_menu where menu_id in (select id from sys_menu where code like 'g4p:%')");
        jdbc.update("delete from sys_menu where code like 'g4p:%'");
    }

    @Test
    void sharedMenuCrudKeepsTreeAndImmutableDefinitionBoundary() throws Exception {
        String token = token("platform");
        var parent = request("POST", MENUS, token, null, JSON.writeValueAsString(menu("0", "平台测试目录", "g4p:directory", 1, 1)));
        assertThat(parent.status()).as(parent.raw()).isEqualTo(200);
        String parentId = parent.body().at("/data/id").asText();

        assertThat(request("POST", MENUS, token, null, JSON.writeValueAsString(menu(parentId,"错误按钮","g4p:invalid",3,1))).status()).isEqualTo(400);
        var page = request("POST", MENUS, token, null, JSON.writeValueAsString(menu(parentId,"平台测试菜单","g4p:page",2,1)));
        assertThat(page.status()).as(page.raw()).isEqualTo(200);
        String pageId=page.body().at("/data/id").asText();
        var child = request("POST", MENUS, token, null, JSON.writeValueAsString(menu(pageId, "平台测试操作", "g4p:action", 3, 1)));
        assertThat(child.status()).as(child.raw()).isEqualTo(200);
        String childId = child.body().at("/data/id").asText();
        assertThat(request("DELETE", MENUS + "/" + parentId + "?version=0", token, null, null).status()).isEqualTo(409);

        var edit = Map.ofEntries(
                Map.entry("id", parentId), Map.entry("version", 0), Map.entry("parentId", childId),
                Map.entry("name", "循环目录"), Map.entry("path", ""), Map.entry("component", ""),
                Map.entry("redirect", ""), Map.entry("icon", ""), Map.entry("sort", 0),
                Map.entry("visible", 1), Map.entry("status", 1), Map.entry("platformOnly", 1),
                Map.entry("keepAlive", 0), Map.entry("alwaysShow", 0));
        assertThat(request("PUT", MENUS, token, null, JSON.writeValueAsString(edit)).status()).isEqualTo(400);

        assertThat(request("DELETE", MENUS + "/" + childId + "?version=0", token, null, null).status()).isEqualTo(200);
        assertThat(request("DELETE", MENUS + "/" + pageId + "?version=0", token, null, null).status()).isEqualTo(200);
        assertThat(request("DELETE", MENUS + "/" + parentId + "?version=0", token, null, null).status()).isEqualTo(200);
        assertThat(request("GET", MENUS, token, null, null).raw()).doesNotContain("g4p:directory", "g4p:action");

        var invalid = menu("0", "错误平台权限", "platform:g4p:bad", 3, 0);
        assertThat(request("POST", MENUS, token, null, JSON.writeValueAsString(invalid)).status()).isEqualTo(400);
        assertThat(request("GET", MENUS, token("alice"), "scope-a", null).status()).isEqualTo(403);
    }

    @Test
    void platformRoleCrudEnforcesPermissionCeilingAndGlobalScope() throws Exception {
        String token = token("platform");
        List<String> menuIds = List.of(menuId("platform:scope:access"), menuId("platform:role:read"));
        var create = Map.of("code", "G4P_AUDITOR", "name", "平台审计员", "status", 1, "sort", 3,
                "remark", "integration", "menuIds", menuIds);
        var created = request("POST", ROLES, token, null, JSON.writeValueAsString(create));
        assertThat(created.status()).as(created.raw()).isEqualTo(200);
        String id = created.body().at("/data/id").asText();
        assertThat(jdbc.queryForObject("select scope_id from sys_role where id=?", String.class, id)).isEqualTo("GLOBAL");
        assertThat(jdbc.queryForObject("select data_range from sys_role where id=?", Integer.class, id)).isOne();

        var detail = request("GET", ROLES + "/" + id, token, null, null);
        assertThat(detail.status()).as(detail.raw()).isEqualTo(200);
        assertThat(detail.body().at("/data/editable").asBoolean()).isTrue();

        var edit = Map.of("id", id, "version", 0, "name", "平台审计员二", "status", 1, "sort", 4,
                "remark", "updated", "menuIds", List.of(menuId("platform:role:read")));
        assertThat(request("PUT", ROLES, token, null, JSON.writeValueAsString(edit)).status()).isEqualTo(200);
        assertThat(request("DELETE", ROLES + "/" + id + "?version=0", token, null, null).status()).isEqualTo(409);
        assertThat(request("DELETE", ROLES + "/" + id + "?version=1", token, null, null).status()).isEqualTo(200);
    }

    @Test
    void finalAdministratorGuardRollsBackMenuRoleAndAccountChanges() throws Exception {
        String token = token("platform");
        var criticalEdit = Map.ofEntries(
                Map.entry("id", "platform-access"), Map.entry("version", 0), Map.entry("parentId", "g1-guard-menu"),
                Map.entry("name", "platform:scope:access"), Map.entry("path", ""), Map.entry("component", ""),
                Map.entry("redirect", ""), Map.entry("icon", ""), Map.entry("sort", 0),
                Map.entry("visible", 1), Map.entry("status", 0), Map.entry("platformOnly", 1),
                Map.entry("keepAlive", 0), Map.entry("alwaysShow", 0));
        assertThat(request("PUT", MENUS, token, null, JSON.writeValueAsString(criticalEdit)).status()).isEqualTo(409);
        assertThat(jdbc.queryForObject("select status from sys_menu where id='platform-access'", Integer.class)).isOne();

        List<String> withoutCritical = MANAGEMENT.stream().map(this::menuId).toList();
        var roleEdit = Map.of("id", "role-platform", "version", 0, "name", "平台操作员", "status", 1,
                "sort", 0, "remark", "", "menuIds", withoutCritical);
        assertThat(request("PUT", ROLES, token, null, JSON.writeValueAsString(roleEdit)).status()).isEqualTo(409);
        assertThat(count("select count(*) from sys_role_menu where role_id='role-platform' and menu_id='platform-access'")).isOne();

        assertThat(request("PUT", ROLES + "/accounts/u-platform/roles", token, null,
                JSON.writeValueAsString(Map.of("roleIds", List.of()))).status()).isEqualTo(409);
        assertThat(count("select count(*) from sys_user_role where user_id='u-platform' and scope_id='GLOBAL'")).isOne();
    }

    @Test
    void secondAdministratorAllowsReplacementWithoutTouchingUnitGrants() throws Exception {
        String token = token("platform");
        List<String> menuIds = java.util.stream.Stream.concat(
                java.util.stream.Stream.of(menuId("platform:scope:access")), MANAGEMENT.stream().map(this::menuId))
                .distinct().toList();
        var create = Map.of("code", "G4P_SECOND", "name", "第二平台管理员", "status", 1, "sort", 1,
                "remark", "", "menuIds", menuIds);
        var created = request("POST", ROLES, token, null, JSON.writeValueAsString(create));
        String roleId = created.body().at("/data/id").asText();
        assertThat(created.status()).as(created.raw()).isEqualTo(200);

        assertThat(request("PUT", ROLES + "/accounts/u-bob/roles", token, null,
                JSON.writeValueAsString(Map.of("roleIds", List.of(roleId)))).status()).isEqualTo(200);
        assertThat(request("PUT", ROLES + "/accounts/u-platform/roles", token, null,
                JSON.writeValueAsString(Map.of("roleIds", List.of()))).status()).isEqualTo(200);
        assertThat(count("select count(*) from sys_user_role where user_id='u-bob' and role_id='role-b' and scope_id='scope-b'")).isOne();
        assertThat(count("select count(*) from sys_user_role where user_id='u-bob' and role_id='" + roleId + "' and scope_id='GLOBAL'")).isOne();
        assertThat(request("GET", ROLES, token, null, null).status()).isEqualTo(403);
    }

    @Test
    void concurrentRevocationsCannotRemoveBothAdministrators() throws Exception {
        String platformToken = token("platform");
        List<String> menuIds = java.util.stream.Stream.concat(
                java.util.stream.Stream.of(menuId("platform:scope:access")), MANAGEMENT.stream().map(this::menuId))
                .distinct().toList();
        var create = Map.of("code", "G4P_CONCURRENT", "name", "并发平台管理员", "status", 1, "sort", 2,
                "remark", "", "menuIds", menuIds);
        var created = request("POST", ROLES, platformToken, null, JSON.writeValueAsString(create));
        String roleId = created.body().at("/data/id").asText();
        assertThat(created.status()).as(created.raw()).isEqualTo(200);
        assertThat(request("PUT", ROLES + "/accounts/u-bob/roles", platformToken, null,
                JSON.writeValueAsString(Map.of("roleIds", List.of(roleId)))).status()).isEqualTo(200);
        String bobToken = token("bob");

        CountDownLatch start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> {
                start.await();
                return request("PUT", ROLES + "/accounts/u-platform/roles", platformToken, null,
                        JSON.writeValueAsString(Map.of("roleIds", List.of()))).status();
            });
            var second = pool.submit(() -> {
                start.await();
                return request("PUT", ROLES + "/accounts/u-bob/roles", bobToken, null,
                        JSON.writeValueAsString(Map.of("roleIds", List.of()))).status();
            });
            start.countDown();
            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(200, 409);
        }
        assertThat(count("select count(distinct user_id) from sys_user_role where scope_id='GLOBAL' and deleted=0")).isOne();
    }

    private Map<String, Object> menu(String parentId, String name, String code, int type, int platformOnly) {
        return Map.ofEntries(Map.entry("parentId", parentId), Map.entry("name", name), Map.entry("code", code),
                Map.entry("type", type), Map.entry("path", ""), Map.entry("component", ""),
                Map.entry("redirect", ""), Map.entry("icon", ""), Map.entry("sort", 0),
                Map.entry("visible", 1), Map.entry("status", 1), Map.entry("platformOnly", platformOnly),
                Map.entry("keepAlive", 0), Map.entry("alwaysShow", 0));
    }

    private void allowPlatform(String code) {
        permission("role-platform", "g1-" + code.replace(':', '-'), code, true);
    }

    private String menuId(String code) {
        return jdbc.queryForObject("select id from sys_menu where code=? and deleted=0", String.class, code);
    }

    private int count(String sql) { return jdbc.queryForObject(sql, Integer.class); }
}
