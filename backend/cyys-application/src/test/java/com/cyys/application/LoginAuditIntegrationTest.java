package com.cyys.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(classes = CyysApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"cyys.data-isolation.mode=multi", "cyys.data-isolation.single-scope-id="})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LoginAuditIntegrationTest extends IsolationTestSupport {
    private static final String PATH = "/api/admin/login-audits";

    @BeforeEach
    void auditPermissions() {
        allow("role-a", "login-audit:read", false);
        allow("role-b", "login-audit:read", false);
    }

    @Test
    void authenticationOutcomesCommitEvenWhenTheHttpRequestFails() throws Exception {
        assertThat(login("alice", "wrong", null).status()).isEqualTo(401);
        assertThat(jdbc.queryForObject("select pwd_error_count from sys_user where id='u-alice'", Integer.class))
                .isEqualTo(1);
        assertThat(login("alice", PASSWORD, null).status()).isEqualTo(200);
        jdbc.update("update sys_user set pwd_lock_time=date_add(now(), interval 10 minute) where id='u-alice'");
        assertThat(login("alice", PASSWORD, null).status()).isEqualTo(423);
        jdbc.update("update sys_user set status=0 where id='u-alice'");
        assertThat(login("alice", PASSWORD, null).status()).isEqualTo(403);
        assertThat(jdbc.queryForList("select outcome from sys_login_audit where user_id='u-alice'", String.class))
                .containsExactlyInAnyOrder("FAILURE", "SUCCESS", "LOCKED", "DISABLED");
        assertThat(count("select count(*) from sys_login_audit where user_id='u-alice' and scope_id='scope-a'"))
                .isEqualTo(4);
        assertThat(jdbc.queryForList("select * from sys_login_audit").toString())
                .doesNotContain(PASSWORD, PASSWORD_HASH, "wrong", "Bearer", "private-id-card");
    }

    @Test
    void forgedOrUnknownScopeNeverPublishesAccountDetailsToAnotherUnit() throws Exception {
        assertThat(login("alice", "wrong", "scope-b").status()).isEqualTo(401);
        assertThat(login("alice", PASSWORD, "scope-b").status()).isEqualTo(403);
        assertThat(login("g4-audit-unknown", PASSWORD, "scope-b").status()).isEqualTo(401);
        assertThat(count("select count(*) from sys_login_audit where scope_id is null")).isEqualTo(3);
        assertThat(count("select count(*) from sys_login_audit where login_name='g4-audit-unknown' and user_id is null"))
                .isEqualTo(1);
        assertThat(count("select count(*) from sys_login_audit where reason='SCOPE_DENIED'"))
                .isEqualTo(1);
        String bob = token("bob");
        Reply page = request("GET", PATH, bob, "scope-b", null);
        assertThat(page.status()).as(page.raw()).isEqualTo(200);
        assertThat(page.body().at("/data/total").asInt()).isEqualTo(1);
        assertThat(page.raw()).doesNotContain("alice", "g4-audit-unknown", "u-alice");
    }

    @Test
    void currentUnitPagingFiltersAndPermissionRevocationUseTheRealBackend() throws Exception {
        assertThat(login("alice", "wrong", null).status()).isEqualTo(401);
        String alice = token("alice");
        token("bob");
        Reply page = request("GET", PATH + "?size=1", alice, "scope-a", null);
        assertThat(page.status()).as(page.raw()).isEqualTo(200);
        assertThat(page.body().at("/data/total").asInt()).isEqualTo(2);
        assertThat(page.body().at("/data/records").size()).isEqualTo(1);
        assertThat(page.body().at("/data/records/0/scopeId").asText()).isEqualTo("scope-a");
        Reply failures = request("GET", PATH + "?loginName=alice&outcome=FAILURE", alice, "scope-a", null);
        assertThat(failures.body().at("/data/total").asInt()).isEqualTo(1);
        assertThat(failures.body().at("/data/records/0/reason").asText()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(request("GET", PATH + "?loginName=missing", alice, "scope-a", null)
                .body().at("/data/total").asInt()).isZero();
        assertThat(request("GET", PATH, null, "scope-a", null).status()).isEqualTo(401);
        assertThat(request("GET", PATH, alice, null, null).status()).isEqualTo(403);
        assertThat(request("GET", PATH, alice, "scope-b", null).status()).isEqualTo(403);
        jdbc.update("update sys_menu set status=0 where code='login-audit:read'");
        assertThat(request("GET", PATH, alice, "scope-a", null).status()).isEqualTo(403);
    }

    @Test
    void bothAuditApisRejectInvalidPagingAndOversizedFilters() throws Exception {
        allow("role-a", "audit:read", false);
        String alice = token("alice");
        for (String path : List.of(PATH, "/api/admin/audits")) {
            for (String query : List.of("page=0", "size=0", "size=-1", "size=201", "size=not-a-number")) {
                Reply reply = request("GET", path + "?" + query, alice, "scope-a", null);
                assertThat(reply.status()).as(path + "?" + query + ": " + reply.raw()).isEqualTo(400);
            }
        }
        for (String query : List.of("loginName=" + "a".repeat(129), "outcome=" + "a".repeat(21))) {
            assertThat(request("GET", PATH + "?" + query, alice, "scope-a", null).status()).isEqualTo(400);
        }
        for (String query : List.of("resource=" + "a".repeat(65), "operation=" + "a".repeat(21))) {
            assertThat(request("GET", "/api/admin/audits?" + query, alice, "scope-a", null).status()).isEqualTo(400);
        }
    }

    @Test
    void lockThresholdAndMalformedLegacyHashesAreRecordedWithoutCreatingSessions() throws Exception {
        jdbc.update("update sys_user set pwd_error_count=4 where id='u-alice'");
        assertThat(login("alice", "wrong", null).status()).isEqualTo(423);
        assertThat(count("select count(*) from sys_login_audit where outcome='LOCKED' and reason='PASSWORD_LOCKED'"))
                .isEqualTo(1);
        jdbc.update("update sys_user set password_hash='unsupported-legacy' where id='u-bob'");
        assertThat(login("bob", PASSWORD, null).status()).isEqualTo(401);
        assertThat(count("select count(*) from sys_login_audit where user_id='u-bob' and outcome='FAILURE'"))
                .isEqualTo(1);
        assertThat(cn.dev33.satoken.stp.StpUtil.isLogin("u-alice")).isFalse();
        assertThat(cn.dev33.satoken.stp.StpUtil.isLogin("u-bob")).isFalse();
    }
}
