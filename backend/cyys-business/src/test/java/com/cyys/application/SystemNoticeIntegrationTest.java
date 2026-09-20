package com.cyys.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(classes=CyysApplication.class, webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties={"cyys.data-isolation.mode=multi","cyys.data-isolation.single-scope-id="})
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class SystemNoticeIntegrationTest extends HttpTestSupport {
    private static final String ROOT = "/api/admin/platform/system-notices";

    @BeforeEach void setupNotices() {
        platformUser();
        for (String action : List.of("read", "create", "update", "delete"))
            permission("role-platform", "g4-system-notice-" + action, "system-notice:" + action, true);
        jdbc.update("insert into param_basic(id,name,system_name) values('98301','系统通知类型','SYSTEM_NOTICE_TYPE')");
        jdbc.update("insert into param_basic_type(id,basic_id,param_key,param_value) values('98302','98301','normal','普通通知')");
    }

    @AfterEach void cleanupNotices() {
        jdbc.update("delete from sys_notice where created_by='u-platform'");
        jdbc.update("delete from param_basic_type where basic_id='98301'");
        jdbc.update("delete from param_basic where id='98301'");
        jdbc.update("delete from sys_data_audit where actor_id='u-platform' and resource='system-notice'");
        jdbc.update("delete from sys_role_menu where menu_id like 'g4-system-notice-%'");
        jdbc.update("delete from sys_menu where id like 'g4-system-notice-%'");
    }

    @Test void globalCrudPreservesContentAndHistoricalTypeWithoutReadTracking() throws Exception {
        String token = token("platform");
        var created = request("POST", ROOT, token, null, command(null, "normal", false));
        assertThat(created.status()).as(created.raw()).isEqualTo(200);
        String id = created.body().at("/data/id").asText();
        assertThat(created.body().at("/data/content").asText()).isEqualTo("第一行\n<script>原样文本</script>");
        for (String scope : List.of("scope-a", "scope-b")) {
            var list = request("GET", ROOT + "?page=1&size=1&title=notice", token, scope, null);
            assertThat(list.status()).as(list.raw()).isEqualTo(200);
            assertThat(list.body().at("/data/records/0/id").asText()).isEqualTo(id);
            assertThat(list.body().at("/data/total").asInt()).isEqualTo(1);
        }
        jdbc.update("update param_basic_type set deleted=1 where id='98302'");
        var historical = request("GET", ROOT + "/" + id, token, null, null);
        assertThat(historical.body().at("/data/typeDisabled").asBoolean()).isTrue();
        assertThat(historical.body().at("/data/typeLabel").asText()).isEqualTo("普通通知");
        assertThat(request("GET", ROOT + "/" + id + "/edit-options", token, null, null).body().at("/data").isEmpty()).isTrue();
        assertThat(request("PUT", ROOT + "/" + id, token, null, command(0, "normal", true)).status()).isEqualTo(200);
        assertThat(request("POST", ROOT, token, null, command(null, "normal", false)).status()).isEqualTo(409);
        assertThat(request("GET", ROOT + "/" + id, token, null, null).body().at("/data/visible").asBoolean()).isTrue();
        assertThat(jdbc.queryForObject("select count(*) from sys_org_notice_ref", Integer.class)).isZero();
        assertThat(jdbc.queryForList("select scope_id from sys_data_audit where resource='system-notice'", String.class))
                .containsOnly("GLOBAL").hasSize(2);
        assertThat(request("DELETE", ROOT + "/" + id + "?version=1", token, null, null).status()).isEqualTo(200);
        assertThat(request("GET", ROOT + "/" + id, token, null, null).status()).isEqualTo(404);
    }

    @Test void requiresLivePlatformAuthorityAndRejectsMalformedCommands() throws Exception {
        String token = token("platform");
        String alice = token("alice");
        jdbc.update("insert into sys_role_menu(id,role_id,menu_id) values('g4-system-notice-unit','role-a','g4-system-notice-read')");
        assertThat(request("GET", ROOT, null, null, null).status()).isEqualTo(401);
        assertThat(request("GET", ROOT, alice, "scope-a", null).status()).isEqualTo(403);
        assertThat(request("GET", ROOT + "?size=201", token, null, null).status()).isEqualTo(400);
        assertThat(request("POST", ROOT, token, null, "{\"title\":\" \"}").status()).isEqualTo(400);
        assertThat(request("POST", ROOT, token, null, command(null, "unknown", false)).status()).isEqualTo(409);
        var result = request("POST", ROOT, token, null, command(null, "normal", false));
        assertThat(result.status()).as(result.raw()).isEqualTo(200);
        String id = result.body().at("/data/id").asText();
        assertThat(request("PUT", ROOT + "/" + id, token, null,
                "{\"version\":0,\"title\":\"notice\",\"content\":\"x\",\"type\":\"normal\"}").status()).isEqualTo(400);
        jdbc.update("delete from sys_role_menu where role_id='role-platform' and menu_id='g4-system-notice-update'");
        assertThat(request("PUT", ROOT + "/" + id, token, null, command(0, "normal", true)).status()).isEqualTo(403);
        assertThat(request("GET", ROOT + "/" + id + "/edit-options", token, null, null).status()).isEqualTo(403);
        assertThat(request("GET", ROOT + "/" + id, token, null, null).body().at("/data/visible").asBoolean()).isFalse();
    }

    @Test void concurrentEditsAndDeletesRequireCurrentVersion() throws Exception {
        String token = token("platform");
        String id = request("POST", ROOT, token, null, command(null, "normal", false)).body().at("/data/id").asText();
        try (var pool = Executors.newFixedThreadPool(2)) {
            List<Callable<Integer>> changes = List.of(
                    () -> request("PUT", ROOT + "/" + id, token, null, command(0, "normal", true)).status(),
                    () -> request("PUT", ROOT + "/" + id, token, null, command(0, "normal", false)).status());
            var results = pool.invokeAll(changes);
            assertThat(List.of(results.get(0).get(), results.get(1).get())).containsExactlyInAnyOrder(200,409);
        }
        assertThat(request("DELETE", ROOT + "/" + id + "?version=0", token, null, null).status()).isEqualTo(409);
        assertThat(request("DELETE", ROOT + "/" + id + "?version=1", token, null, null).status()).isEqualTo(200);
    }

    private String command(Integer version, String type, boolean visible) throws Exception {
        var body = new java.util.LinkedHashMap<String,Object>(Map.of("title", "notice", "content", "第一行\n<script>原样文本</script>", "type", type, "visible", visible));
        if (version != null) body.put("version", version);
        return JSON.writeValueAsString(body);
    }
}
