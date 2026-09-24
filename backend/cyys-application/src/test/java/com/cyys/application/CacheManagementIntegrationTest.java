package com.cyys.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(classes = CyysApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"cyys.data-isolation.mode=multi", "cyys.data-isolation.single-scope-id="})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CacheManagementIntegrationTest extends IsolationTestSupport {
    private static final String PATH = "/api/admin/caches";

    @BeforeEach
    void cachePermissions() {
        allow("role-a", "cache:user:refresh", false);
        allow("role-a", "user:display:read", false);
        allow("role-b", "user:display:read", false);
    }

    @Test
    void registeredRefreshEvictsSharedUserRecordsAndKeepsOtherGroups() throws Exception {
        String alice = token("alice"), bob = token("bob");
        ok("GET", "/user-names?ids=u-alice", alice, "scope-a", null);
        ok("GET", "/user-names?ids=u-bob", bob, "scope-b", null);
        as("u-alice", "scope-a"); String a = keys.platform("user-records");
        com.cyys.common.satoken.ScopeContext.clear();
        as("u-bob", "scope-b"); String b = keys.unit("department-records");
        com.cyys.common.satoken.ScopeContext.clear();
        String platformKey = keys.platform("g4-cache-control-fixture");
        String sessionKey = keys.sessionPrefix() + "g4-cache-control-fixture";
        try {
            redis.opsForValue().set(b, "department-sentinel", Duration.ofMinutes(2));
            redis.opsForValue().set(platformKey, "platform-sentinel", Duration.ofMinutes(2));
            redis.opsForValue().set(sessionKey, "session-sentinel", Duration.ofMinutes(2));
            Reply actions = request("GET", PATH, alice, "scope-a", null);
            assertThat(actions.status()).as(actions.raw()).isEqualTo(200);
            assertThat(actions.body().at("/data").size()).isGreaterThanOrEqualTo(6);
            assertThat(actions.raw()).contains("\"cacheKey\":\"user-records\"", "\"groupName\":\"用户\"");
            assertThat(actions.raw()).doesNotContain("redis", "methodName", "java.lang", "USER_MAP_FULL", "refresh-items");
            Reply refresh = request("POST", PATH + "/refresh-groups/refresh", alice, "scope-a",
                    JSON.writeValueAsString(Map.of("refreshGroup", "用户缓存", "scope", "SYSTEM")));
            assertThat(refresh.status()).as(refresh.raw()).isEqualTo(200);
            assertThat(redis.hasKey(a)).isFalse();
            assertThat(redis.hasKey(b)).isTrue();
            assertThat(redis.opsForValue().get(platformKey)).isEqualTo("platform-sentinel");
            assertThat(redis.opsForValue().get(sessionKey)).isEqualTo("session-sentinel");
            assertThat(request("GET", "/api/auth/me", alice, "scope-a", null).status()).isEqualTo(200);
            assertThat(count("select count(*) from sys_data_audit where resource='cache' and operation='REFRESH' "
                    + "and scope_id='GLOBAL' and user_id='u-alice'"))
                    .isEqualTo(1);
            assertThat(ok("GET", "/user-names?ids=u-alice", alice, "scope-a", null).raw()).contains("alice");
            assertThat(redis.hasKey(a)).isTrue();
            assertThat(redis.getExpire(a)).isBetween(1L, 600L);
        } finally { redis.delete(List.of(platformKey, sessionKey, b)); }
    }

    @Test
    void unregisteredActionsScopeForgeryAndRevokedPermissionsAreRejected() throws Exception {
        String alice = token("alice"), bob = token("bob");
        assertThat(request("GET", PATH, null, "scope-a", null).status()).isEqualTo(401);
        assertThat(request("GET", PATH, alice, null, null).status()).isEqualTo(403);
        assertThat(request("POST", PATH + "/refresh-groups/refresh", alice, "scope-b",
                JSON.writeValueAsString(Map.of("refreshGroup", "用户缓存", "scope", "SYSTEM"))).status()).isEqualTo(403);
        assertThat(request("POST", PATH + "/refresh-groups/refresh", bob, "scope-b",
                JSON.writeValueAsString(Map.of("refreshGroup", "用户缓存", "scope", "SYSTEM"))).status()).isEqualTo(403);
        for (String action : List.of("all", "sessions", "USER_MAP_FULL", "java.lang.Runtime")) {
            assertThat(request("POST", PATH + "/" + action + "/refresh", alice, "scope-a", null).status()).isEqualTo(404);
        }
        assertThat(count("select count(*) from sys_data_audit where resource='cache'")).isZero();
        jdbc.update("update sys_menu set status=0 where code='cache:user:refresh'");
        assertThat(request("GET", PATH, alice, "scope-a", null).status()).isEqualTo(403);
        assertThat(request("POST", PATH + "/refresh-groups/refresh", alice, "scope-a",
                JSON.writeValueAsString(Map.of("refreshGroup", "用户缓存", "scope", "SYSTEM"))).status()).isEqualTo(403);
    }
}
