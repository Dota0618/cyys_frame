package com.cyys.application;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest(classes = CyysApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"cyys.data-isolation.mode=multi", "cyys.data-isolation.single-scope-id="})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MultiScopeIntegrationTest extends HttpTestSupport {
    @Test
    void selectionCannotGrantMembershipOrFallBackToDefault() throws Exception {
        String token = token("alice");
        assertThat(request("GET", "/api/auth/me", token, "scope-b", null).status()).isEqualTo(403);
        assertThat(request("GET", "/api/auth/me", token, "missing", null).status()).isEqualTo(403);
        assertThat(request("GET", "/api/test/permission", token, null, null).status()).isEqualTo(403);
        Reply unselected = request("GET", "/api/auth/me", token, null, null);
        assertThat(unselected.status()).as(unselected.raw()).isEqualTo(200);
        assertThat(unselected.body().at("/data/currentScopeId").isMissingNode()).isTrue();
        assertThat(unselected.body().at("/data/permissions").isEmpty()).isTrue();
        assertThat(login("alice", PASSWORD, "scope-b").status()).isEqualTo(403);
    }

    @Test
    void twoTabsAndConcurrentRequestsKeepTheirOwnScopeAndPermissions() throws Exception {
        membership("u-alice", "scope-b", false);
        grant("u-alice", "role-b", "scope-b");
        String token = token("alice");
        List<java.util.concurrent.Future<Reply>> replies = new ArrayList<>();
        try (var executor = Executors.newFixedThreadPool(6)) {
            for (int index = 0; index < 20; index++) {
                String scope = index % 2 == 0 ? "scope-a" : "scope-b";
                replies.add(executor.submit(() -> request("GET", "/api/auth/me", token, scope, null)));
            }
            for (int index = 0; index < replies.size(); index++) {
                Reply reply = replies.get(index).get();
                boolean firstScope = index % 2 == 0;
                assertThat(reply.status()).as(reply.raw()).isEqualTo(200);
                assertThat(reply.body().at("/data/currentScopeId").asText()).isEqualTo(firstScope ? "scope-a" : "scope-b");
                assertThat(reply.body().at("/data/permissions").toString())
                        .contains(firstScope ? "test:read" : "test:write")
                        .doesNotContain(firstScope ? "test:write" : "test:read");
            }
        }
        assertThat(request("GET", "/api/test/permission", token, "scope-a", null).status()).isEqualTo(200);
        assertThat(request("GET", "/api/test/permission", token, "scope-b", null).status()).isEqualTo(403);
    }

    @Test
    void platformAuthorityIsExplicitAndNeverCreatesFakeOwnerMembership() throws Exception {
        platformUser();
        Reply login = login("platform", PASSWORD, null);
        assertThat(login.status()).as(login.raw()).isEqualTo(200);
        assertThat(login.raw()).doesNotContain("OWNER", "platformWriteAll");
        assertThat(login.body().at("/data/userInfo/currentScopeId").isMissingNode()).isTrue();
        assertThat(login.body().at("/data/userInfo/scopes").size()).isEqualTo(2);
        String token = login.body().at("/data/token").asText();
        assertThat(request("GET", "/api/test/permission", token, null, null).status()).isEqualTo(403);
        Reply target = request("GET", "/api/test/permission", token, "scope-b", null);
        assertThat(target.status()).as(target.raw()).isEqualTo(200);
        assertThat(target.body().at("/data/loginId").asText()).isEqualTo("u-platform");
        assertThat(target.body().at("/data/scopeId").asText()).isEqualTo("scope-b");
        jdbc.update("update sys_role_menu set deleted=1 where menu_id='platform-access'");
        assertThat(request("GET", "/api/auth/me", token, "scope-b", null).status()).isEqualTo(403);
    }

    @Test
    void unitRoleCannotGrantPlatformOnlyPermissionAndOldIdHasNoPrivilege() throws Exception {
        permission("role-a", "platform-access", "platform:scope:access", true);
        String token = token("alice");
        assertThat(request("GET", "/api/auth/me", token, "scope-b", null).status()).isEqualTo(403);
        Reply alice = request("GET", "/api/auth/me", token, "scope-a", null);
        assertThat(alice.body().at("/data/platformPermissions").isEmpty()).isTrue();
        assertThat(alice.body().at("/data/permissions").toString()).doesNotContain("platform:scope:access");

        user("1", "old-root");
        membership("1", "scope-a", true);
        String oldRoot = token("old-root");
        assertThat(request("GET", "/api/auth/me", oldRoot, "scope-b", null).status()).isEqualTo(403);
    }

    @Test
    void serviceRejectsRoleGrantsWithMismatchedScope() {
        permission("role-a", "g1-grant", "role:grant", false);
        com.cyys.common.satoken.ScopeContext.set(new com.cyys.common.satoken.RequestIdentity(
                "u-alice", "scope-a", List.of(), List.of(), List.of()));
        try {
            assertThatThrownBy(() -> roleGrants.replaceUnitRoles("u-alice", List.of("role-b")))
                    .isInstanceOf(com.cyys.common.web.ApiException.class);
        } finally {
            com.cyys.common.satoken.ScopeContext.clear();
        }
        assertThat(jdbc.queryForObject("select count(*) from sys_user_role where user_id='u-alice'", Integer.class)).isEqualTo(1);
    }

    @org.springframework.beans.factory.annotation.Autowired
    com.cyys.admin.role.service.RoleGrantService roleGrants;

    @Test
    void deletedRolesAndExpiredMembershipsDoNotSurviveInPermissionSnapshots() throws Exception {
        String token = token("alice");
        jdbc.update("update sys_role set deleted=1 where id='role-a'");
        assertThat(request("GET", "/api/test/permission", token, "scope-a", null).status()).isEqualTo(403);
        jdbc.update("update sys_user_scope set expire_at=? where user_id='u-alice'", java.time.LocalDateTime.now().minusMinutes(1));
        assertThat(request("GET", "/api/auth/me", token, "scope-a", null).status()).isEqualTo(403);
    }
}
