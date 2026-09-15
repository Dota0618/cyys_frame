package com.cyys.application;

import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import java.net.URI;
import java.net.http.HttpRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(classes = CyysApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SingleScopeIntegrationTest extends HttpTestSupport {
    @org.springframework.beans.factory.annotation.Autowired
    org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping mappings;

    @Test
    void onlyAcceptedProductionControllersAreRegistered() {
        assertThat(mappings.getHandlerMethods().values().stream()
                .map(org.springframework.web.method.HandlerMethod::getBeanType)
                .filter(type -> type.getPackageName().startsWith("com.cyys"))
                .filter(type -> type != PermissionProbe.class && type != IsolationProbe.class)
                .map(Class::getSimpleName).distinct().toList())
                .containsExactlyInAnyOrder("AuthController", "HealthController", "MemberController",
                        "DepartmentController", "RoleController");
    }

    @Test
    void bearerHeaderIsRequiredAndExpiredTokensAreDenied() throws Exception {
        String token = token("alice");
        Reply cookie = send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/me"))
                .header("Cookie", "Authorization=" + token).GET().build());
        assertThat(cookie.status()).isEqualTo(401);
        Reply rawToken = send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/me"))
                .header("Authorization", token).GET().build());
        assertThat(rawToken.status()).isEqualTo(401);
        assertThat(request("GET", "/api/auth/login-extra", null, null, null).status()).isEqualTo(401);
        long originalTimeout = cn.dev33.satoken.SaManager.getConfig().getTimeout();
        String expiring;
        try {
            cn.dev33.satoken.SaManager.getConfig().setTimeout(1L);
            expiring = token("alice");
        } finally {
            cn.dev33.satoken.SaManager.getConfig().setTimeout(originalTimeout);
        }
        Thread.sleep(1250);
        assertThat(request("GET", "/api/auth/me", expiring, null, null).status()).isEqualTo(401);
    }

    @Test
    void loginAndMeOnlyReturnAllowedFieldsAndFixedScope() throws Exception {
        membership("u-alice", "scope-b", false);
        Reply login = login("alice", PASSWORD, null);
        assertThat(login.status()).as(login.raw()).isEqualTo(200);
        assertThat(login.cookies()).isEmpty();
        assertThat(login.raw()).doesNotContain("passwordHash", "password_hash", "idCard", PASSWORD_HASH);
        assertThat(login.body().at("/data/tokenType").asText()).isEqualTo("Bearer");
        assertThat(login.body().at("/data/userInfo/currentScopeId").asText()).isEqualTo("scope-a");
        assertThat(login.body().at("/data/userInfo/scopes").size()).isEqualTo(1);
        String token = login.body().at("/data/token").asText();
        Reply me = request("GET", "/api/auth/me", token, null, null);
        assertThat(me.status()).as(me.raw()).isEqualTo(200);
        assertThat(me.body().at("/data/currentScopeId").asText()).isEqualTo("scope-a");
        assertThat(me.body().at("/data/permissions").toString()).contains("test:read").doesNotContain("test:write");
    }

    @Test
    void invalidInputAndWrongPasswordsDoNotCreateSessions() throws Exception {
        for (String body : List.of("{}", "{\"loginName\":\"alice\"}",
                "{\"loginName\":\" \",\"password\":\" \"}", "{")) {
            Reply reply = request("POST", "/api/auth/login", null, null, body);
            assertThat(reply.status()).as(reply.raw()).isEqualTo(400);
            assertThat(reply.body().path("code").asInt()).isEqualTo(400);
        }
        assertThat(login("alice", "wrong", null).status()).isEqualTo(401);
        assertThat(login("unknown", PASSWORD, null).status()).isEqualTo(401);
        assertThat(StpUtil.isLogin("u-alice")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"removed", "expired", "disabled-member", "disabled-scope", "deleted-scope", "disabled-user"})
    void invalidIdentityCannotLogin(String condition) throws Exception {
        switch (condition) {
            case "removed" -> jdbc.update("delete from sys_user_scope where user_id='u-alice'");
            case "expired" -> jdbc.update("update sys_user_scope set expire_at=? where user_id='u-alice'", LocalDateTime.now().minusDays(1));
            case "disabled-member" -> jdbc.update("update sys_user_scope set status=0 where user_id='u-alice'");
            case "disabled-scope" -> jdbc.update("update sys_scope set status=0 where id='scope-a'");
            case "deleted-scope" -> jdbc.update("update sys_scope set deleted=1 where id='scope-a'");
            case "disabled-user" -> jdbc.update("update sys_user set status=0 where id='u-alice'");
            default -> throw new AssertionError(condition);
        }
        Reply reply = login("alice", PASSWORD, null);
        assertThat(reply.status()).as(reply.raw()).isEqualTo(403);
        assertThat(StpUtil.isLogin("u-alice")).isFalse();
    }

    @Test
    void singleModeRejectsOtherScopesIncludingPlatformActor() throws Exception {
        String token = token("alice");
        assertThat(request("GET", "/api/auth/me", token, "scope-b", null).status()).isEqualTo(403);
        assertThat(login("alice", PASSWORD, "unit-a").status()).isEqualTo(403);
        assertThat(login("bob", PASSWORD, null).status()).isEqualTo(403);
        platformUser();
        String platformToken = token("platform");
        Reply platformMe = request("GET", "/api/auth/me", platformToken, null, null);
        assertThat(platformMe.body().at("/data/scopes").size()).isEqualTo(1);
        assertThat(platformMe.raw()).doesNotContain("OWNER");
        assertThat(request("GET", "/api/auth/me", platformToken, "scope-b", null).status()).isEqualTo(403);
    }

    @Test
    void realPermissionCallbackReflectsRevocationOnNextRequest() throws Exception {
        String token = token("alice");
        assertThat(request("GET", "/api/test/permission", token, null, null).status()).isEqualTo(200);
        jdbc.update("update sys_role_menu set deleted=1 where role_id='role-a'");
        Reply denied = request("GET", "/api/test/permission", token, null, null);
        assertThat(denied.status()).as(denied.raw()).isEqualTo(403);
        Reply me = request("GET", "/api/auth/me", token, null, null);
        assertThat(me.body().at("/data/permissions").isEmpty()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"password", "reset-time", "disabled", "deleted"})
    void accountAndCredentialChangesInvalidateExistingRequests(String condition) throws Exception {
        String token = token("alice");
        switch (condition) {
            case "password" -> jdbc.update("update sys_user set password_hash='changed-password' where id='u-alice'");
            case "reset-time" -> jdbc.update("update sys_user set pwd_last_changed=? where id='u-alice'", LocalDateTime.now());
            case "disabled" -> jdbc.update("update sys_user set status=0 where id='u-alice'");
            case "deleted" -> jdbc.update("update sys_user set deleted=1 where id='u-alice'");
            default -> throw new AssertionError(condition);
        }
        assertThat(request("GET", "/api/auth/me", token, null, null).status()).isEqualTo(401);
    }

    @Test
    void removedMembershipIsDeniedButDoesNotPreventLogout() throws Exception {
        String token = token("alice");
        jdbc.update("delete from sys_user_scope where user_id='u-alice'");
        assertThat(request("GET", "/api/auth/me", token, null, null).status()).isEqualTo(403);
        assertThat(request("POST", "/api/auth/logout", token, null, null).status()).isEqualTo(200);
        assertThat(request("GET", "/api/auth/me", token, null, null).status()).isEqualTo(401);
    }

    @Test
    void invalidAndRevokedTokensAreUnauthorized() throws Exception {
        assertThat(request("GET", "/api/auth/me", null, null, null).status()).isEqualTo(401);
        assertThat(request("GET", "/api/auth/me", "unknown-token", null, null).status()).isEqualTo(401);
        String token = token("alice");
        StpUtil.logoutByTokenValue(token);
        assertThat(request("GET", "/api/auth/me", token, null, null).status()).isEqualTo(401);
    }

    @Test
    void managementCrudIsNotRegistered() throws Exception {
        String token = token("alice");
        for (String feature : List.of("user", "role", "menu", "org")) {
            Reply result = request("GET", "/api/" + feature + "/list", token, null, null);
            assertThat(result.status()).as(result.raw()).isEqualTo(404);
            assertThat(request("POST", "/api/" + feature, token, null, "{}").status()).isIn(404, 405);
        }
        assertThat(jdbc.queryForObject("select count(*) from sys_user", Integer.class)).isEqualTo(2);
    }

    @Test
    void concurrentFailuresCannotLoseTheAccountLockCounter() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        List<java.util.concurrent.Future<Integer>> results = new ArrayList<>();
        try (var pool = Executors.newFixedThreadPool(8)) {
            for (int index = 0; index < 8; index++) {
                results.add(pool.submit(() -> { start.await(); return login("alice", "wrong", null).status(); }));
            }
            start.countDown();
            for (var result : results) {
                assertThat(result.get()).isIn(401, 423);
            }
        }
        assertThat(jdbc.queryForObject("select pwd_error_count from sys_user where id='u-alice'", Integer.class)).isEqualTo(5);
        assertThat(login("alice", PASSWORD, null).status()).isEqualTo(423);
        assertThat(StpUtil.isLogin("u-alice")).isFalse();

        jdbc.update("update sys_user set pwd_lock_time=? where id='u-alice'", LocalDateTime.now().minusMinutes(1));
        assertThat(login("alice", "wrong", null).status()).isEqualTo(401);
        assertThat(jdbc.queryForObject("select pwd_error_count from sys_user where id='u-alice'", Integer.class)).isEqualTo(1);
        assertThat(login("alice", PASSWORD, null).status()).isEqualTo(200);
        assertThat(jdbc.queryForObject("select pwd_error_count from sys_user where id='u-alice'", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select pwd_lock_time from sys_user where id='u-alice'", LocalDateTime.class)).isNull();
    }

    @Test
    void legacyHashIsNotAcceptedAndForwardedIpIsNotTrusted() throws Exception {
        jdbc.update("update sys_user set password_hash='legacy-md5' where id='u-alice'");
        assertThat(login("alice", PASSWORD, null).status()).isEqualTo(401);
        assertThat(StpUtil.isLogin("u-alice")).isFalse();
        jdbc.update("update sys_user set password_hash=? where id='u-alice'", PASSWORD_HASH);
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .header("X-Forwarded-For", "203.0.113.77")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(
                        java.util.Map.of("loginName", "alice", "password", PASSWORD)))).build();
        assertThat(send(request).status()).isEqualTo(200);
        assertThat(jdbc.queryForObject("select last_login_ip from sys_user where id='u-alice'", String.class))
                .isNotEqualTo("203.0.113.77");
    }
}
