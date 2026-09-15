package com.cyys.application;

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
class DepartmentIntegrationTest extends IsolationTestSupport {
    static final String DEPARTMENTS = "/api/admin/departments";

    Reply departments(String method, String suffix, String token, String scope, Object body) throws Exception {
        return request(method, DEPARTMENTS + suffix, token, scope,
                body == null ? null : JSON.writeValueAsString(body));
    }

    @Test
    void pageDetailAndParentChoicesUseCurrentScopeAndStableOrder() throws Exception {
        String token = token("alice");
        var page = departments("GET", "?size=2", token, "scope-a", null);
        assertThat(page.status()).as(page.raw()).isEqualTo(200);
        assertThat(page.body().at("/data/total").asInt()).isEqualTo(3);
        assertThat(page.body().at("/data/records/0/id").asText()).isEqualTo("g1-a-child");
        assertThat(page.raw()).doesNotContain("g1-b-root");
        assertThat(departments("GET", "/g1-b-root", token, "scope-a", null).status()).isEqualTo(404);
        assertThat(departments("GET", "", token, null, null).status()).isEqualTo(403);

        var parents = departments("GET", "/parents", token, "scope-a", null);
        assertThat(parents.status()).as(parents.raw()).isEqualTo(200);
        assertThat(parents.body().path("data").size()).isEqualTo(3);
        assertThat(parents.raw()).contains("parentId").doesNotContain("g1-b-root");
    }

    @Test
    void createEditAndDeleteKeepServerOwnedScopeAndVersionBoundary() throws Exception {
        String token = token("alice");
        assertThat(departments("POST", "", token, "scope-a",
                Map.of("name", "越界", "code", "cross", "parentId", "g1-b-root")).status()).isEqualTo(403);
        assertThat(departments("POST", "", token, "scope-a",
                Map.of("name", "非法归属", "code", "owned", "scopeId", "scope-b")).status()).isEqualTo(400);

        var created = departments("POST", "", token, "scope-a",
                Map.of("name", "新部门", "code", "new-department", "parentId", "g1-a-root"));
        assertThat(created.status()).as(created.raw()).isEqualTo(200);
        String id = created.body().at("/data/id").asText();
        assertThat(created.body().at("/data/scopeId").asText()).isEqualTo("scope-a");
        assertThat(created.body().at("/data/parentId").asText()).isEqualTo("g1-a-root");

        var edited = departments("PUT", "", token, "scope-a",
                Map.of("id", id, "version", 0, "name", "新部门已修改"));
        assertThat(edited.status()).as(edited.raw()).isEqualTo(200);
        assertThat(edited.body().at("/data/name").asText()).isEqualTo("新部门已修改");
        assertThat(departments("PUT", "", token, "scope-a",
                Map.of("id", id, "version", 0, "name", "过期修改")).status()).isEqualTo(409);

        var deleted = departments("DELETE", "", token, "scope-a",
                List.of(Map.of("id", id, "version", 1)));
        assertThat(deleted.status()).as(deleted.raw()).isEqualTo(200);
        assertThat(departments("GET", "/" + id, token, "scope-a", null).status()).isEqualTo(404);
    }

    @Test
    void hiddenWriteCallsAreDeniedAndReferencedDepartmentsCannotBeDeleted() throws Exception {
        String token = token("alice");
        assertThat(departments("DELETE", "", token, "scope-a",
                List.of(Map.of("id", "g1-a-root", "version", 0))).status()).isEqualTo(409);

        jdbc.update("delete from sys_role_menu where role_id='role-a' and menu_id in "
                + "(select id from sys_menu where code='department:create')");
        assertThat(departments("GET", "/parents", token, "scope-a", null).status()).isEqualTo(403);
        assertThat(departments("POST", "", token, "scope-a",
                Map.of("name", "隐藏按钮调用", "code", "hidden")).status()).isEqualTo(403);
        assertThat(departments("GET", "", token, "scope-a", null).status()).isEqualTo(200);
    }
}
