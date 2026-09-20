package com.cyys.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(classes = CyysApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"cyys.data-isolation.mode=multi", "cyys.data-isolation.single-scope-id="})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuditIntegrationTest extends IsolationTestSupport {
    @BeforeEach
    void auditPermissions() {
        allow("role-a", "audit:read", false);
        allow("role-b", "audit:read", false);
    }

    @Test
    void auditReadIsCurrentScopeOnlyAndSupportsBoundedFilters() throws Exception {
        String alice = token("alice");
        String bob = token("bob");
        var createdA = request("POST", "/api/admin/departments", alice, "scope-a",
                JSON.writeValueAsString(java.util.Map.of("name", "audit-a", "code", "audit-a")));
        assertThat(createdA.status()).as(createdA.raw()).isEqualTo(200);
        var createdB = request("POST", "/api/admin/departments", bob, "scope-b",
                JSON.writeValueAsString(java.util.Map.of("name", "audit-b", "code", "audit-b")));
        assertThat(createdB.status()).as(createdB.raw()).isEqualTo(200);

        var page = request("GET", "/api/admin/audits?size=1&resource=department&operation=CREATE",
                alice, "scope-a", null);
        assertThat(page.status()).as(page.raw()).isEqualTo(200);
        assertThat(page.body().at("/data/total").asInt()).isEqualTo(1);
        assertThat(page.body().at("/data/records/0/scopeId").asText()).isEqualTo("scope-a");
        assertThat(page.body().at("/data/records/0/actorId").asText()).isEqualTo("u-alice");
        assertThat(page.body().at("/data/records/0/operation").asText()).isEqualTo("CREATE");
    }

    @Test
    void auditReadRequiresCurrentScopePermission() throws Exception {
        String alice = token("alice");
        assertThat(request("GET", "/api/admin/audits", alice, "scope-a", null).status()).isEqualTo(200);
        jdbc.update("update sys_menu set status=0 where code='audit:read'");
        assertThat(request("GET", "/api/admin/audits", alice, "scope-a", null).status()).isEqualTo(403);
        assertThat(request("GET", "/api/admin/audits", alice, "scope-b", null).status()).isEqualTo(403);
    }
}
