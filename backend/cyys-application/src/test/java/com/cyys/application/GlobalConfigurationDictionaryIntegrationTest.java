package com.cyys.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(classes = CyysApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"cyys.data-isolation.mode=multi", "cyys.data-isolation.single-scope-id="})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GlobalConfigurationDictionaryIntegrationTest extends HttpTestSupport {
    private static final String CONFIG = "/api/admin/platform/configurations";
    private static final String DICT = "/api/admin/platform/dictionaries";
    private static final String FIRST = "990000000001";
    private static final String SECOND = "990000000002";

    @BeforeEach
    void permissions() {
        platformUser();
        for (String resource : List.of("configuration", "dictionary")) {
            for (String action : List.of("read", "create", "update", "delete")) {
                permission("role-platform", "g4-global-" + resource + "-" + action,
                        resource + ":" + action, true);
            }
        }
    }

    @AfterEach
    void cleanupGlobalFixtures() {
        jdbc.update("delete from param_basic_type where basic_id in (?,?)", FIRST, SECOND);
        jdbc.update("delete from param_basic where id in (?,?)", FIRST, SECOND);
        jdbc.update("delete from sys_configure where name like 'g4-config-%'");
        jdbc.update("delete from sys_data_audit where user_id='u-platform' and scope_id='GLOBAL' "
                + "and resource in ('configuration','dictionary','dictionary-item')");
        jdbc.update("delete from sys_role_menu where menu_id like 'g4-global-%'");
        jdbc.update("delete from sys_menu where id like 'g4-global-%'");
    }

    @Test
    void globalConfigurationPreservesNullEmptyFalseAndZeroAcrossUnitSelections() throws Exception {
        String token = token("platform");
        String id = createConfig(token, "g4-config-values", "initial");
        int version = 0;
        for (String value : new String[]{"", "0", "false", null}) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("version", version++);
            body.put("value", value);
            body.put("type", "SYSTEM");
            Reply updated = request("PUT", CONFIG + "/" + id, token, "scope-a", JSON.writeValueAsString(body));
            assertThat(updated.status()).as(updated.raw()).isEqualTo(200);
            assertThat(jdbc.queryForObject("select `value` from sys_configure where id=?", String.class, id))
                    .isEqualTo(value);
            Reply fromB = request("GET", CONFIG + "/" + id, token, "scope-b", null);
            assertThat(fromB.status()).as(fromB.raw()).isEqualTo(200);
            if (value == null) assertThat(fromB.body().at("/data/value").isNull()).isTrue();
            else assertThat(fromB.body().at("/data/value").asText()).isEqualTo(value);
        }
        assertThat(request("GET", CONFIG + "/missing", token, null, null).status()).isEqualTo(404);
        Reply page = request("GET", CONFIG + "?page=1&size=1&name=g4-config", token, null, null);
        assertThat(page.body().at("/data/total").asInt()).isEqualTo(1);
        assertThat(page.body().at("/data/records/0/id").asText()).isEqualTo(id);
    }

    @Test
    void unitPermissionCannotManageGlobalDataAndRevocationTakesEffect() throws Exception {
        String token = token("platform");
        String id = createConfig(token, "g4-config-auth", "keep");
        // 即使单位角色关联同名操作权限，也不能变成 GLOBAL 管理授权。
        jdbc.update("insert into sys_role_menu(id,role_id,menu_id) values"
                + "('g4-global-unit-read','role-a',(select id from sys_menu where code='configuration:read'))");
        String ordinary = token("alice");
        assertThat(request("GET", CONFIG, ordinary, "scope-a", null).status()).isEqualTo(403);
        assertThat(request("POST", DICT, ordinary, "scope-a", dictionaryCommand(FIRST)).status()).isEqualTo(403);
        assertThat(request("GET", CONFIG, null, null, null).status()).isEqualTo(401);
        jdbc.update("delete from sys_role_menu where role_id='role-platform' and menu_id=(select id from sys_menu where code='configuration:update')");
        assertThat(request("PUT", CONFIG + "/" + id, token, null,
                JSON.writeValueAsString(Map.of("version", 0, "value", "changed", "type", "SYSTEM"))).status()).isEqualTo(403);
        assertThat(jdbc.queryForObject("select `value` from sys_configure where id=?", String.class, id)).isEqualTo("keep");
    }

    @Test
    void concurrentConfigurationEditsRequireTheCurrentVersion() throws Exception {
        String token = token("platform");
        String id = createConfig(token, "g4-config-race", "before");
        try (var executor = Executors.newFixedThreadPool(2)) {
            List<Callable<Integer>> edits = List.of(
                    () -> request("PUT", CONFIG + "/" + id, token, null,
                            JSON.writeValueAsString(Map.of("version", 0, "value", "one", "type", "SYSTEM"))).status(),
                    () -> request("PUT", CONFIG + "/" + id, token, null,
                            JSON.writeValueAsString(Map.of("version", 0, "value", "two", "type", "SYSTEM"))).status());
            var outcomes = executor.invokeAll(edits);
            assertThat(List.of(outcomes.get(0).get(), outcomes.get(1).get())).containsExactlyInAnyOrder(200, 409);
        }
        assertThat(request("DELETE", CONFIG + "/" + id + "?version=0", token, null, null).status()).isEqualTo(409);
        assertThat(request("GET", CONFIG + "/" + id, token, null, null).status()).isEqualTo(200);
    }

    @Test
    void dictionaryItemsKeepTheirCategoryAndPreventDeletingAnOccupiedCategory() throws Exception {
        String token = token("platform");
        createDictionary(token, FIRST);
        createDictionary(token, SECOND);
        String item = createItem(token, FIRST, "0");
        assertThat(request("POST", DICT + "/" + FIRST + "/items", token, null, itemCommand("0")).status()).isEqualTo(409);
        createItem(token, SECOND, "0"); // 编码只在同一分类内唯一。
        assertThat(request("DELETE", DICT + "/" + FIRST + "?version=0", token, null, null).status()).isEqualTo(409);
        assertThat(request("GET", DICT + "/" + SECOND + "/items/" + item, token, null, null).status()).isEqualTo(404);
        assertThat(request("DELETE", DICT + "/" + SECOND + "/items/" + item + "?version=0", token, null, null).status()).isEqualTo(409);
        assertThat(request("DELETE", DICT + "/" + FIRST + "/items/" + item + "?version=0", token, null, null).status()).isEqualTo(200);
        assertThat(request("DELETE", DICT + "/" + FIRST + "?version=0", token, null, null).status()).isEqualTo(200);
        assertThat(request("POST", DICT + "/" + FIRST + "/items", token, null, itemCommand("new")).status()).isEqualTo(404);
        assertThat(jdbc.queryForObject("select param_key from param_basic_type where id=?", String.class, item)).isEqualTo("0");
    }

    @Test
    void dictionaryStableIdentifiersAndVersionsSurviveEdits() throws Exception {
        String token = token("platform");
        createDictionary(token, FIRST);
        String item = createItem(token, FIRST, "0");
        Reply updated = request("PUT", DICT + "/" + FIRST, token, null,
                JSON.writeValueAsString(Map.of("version", 0, "name", "新名称")));
        assertThat(updated.status()).as(updated.raw()).isEqualTo(200);
        assertThat(updated.body().at("/data/id").asText()).isEqualTo(FIRST);
        assertThat(updated.body().at("/data/systemName").asText()).isEqualTo(FIRST);
        Reply itemUpdate = request("PUT", DICT + "/" + FIRST + "/items/" + item, token, null,
                JSON.writeValueAsString(Map.of("version", 0, "paramValue", "新字典值")));
        assertThat(itemUpdate.status()).as(itemUpdate.raw()).isEqualTo(200);
        assertThat(itemUpdate.body().at("/data/paramKey").asText()).isEqualTo("0");
        assertThat(request("PUT", DICT + "/" + FIRST + "/items/" + item, token, null,
                JSON.writeValueAsString(Map.of("version", 0, "paramValue", "过期"))).status()).isEqualTo(409);
        Reply page = request("GET", DICT + "/" + FIRST + "/items?page=1&size=1", token, null, null);
        assertThat(page.body().at("/data/total").asInt()).isEqualTo(1);
        assertThat(page.body().at("/data/records/0/paramValue").asText()).isEqualTo("新字典值");
    }

    @Test
    void malformedInputsAndDuplicateKeysFailAtTheServerBoundary() throws Exception {
        String token = token("platform");
        String id = createConfig(token, "g4-config-unique", "");
        assertThat(request("PUT", CONFIG + "/" + id, token, null,
                JSON.writeValueAsString(Map.of("version", 0, "type", "SYSTEM"))).status()).isEqualTo(400);
        assertThat(request("POST", CONFIG, token, null,
                JSON.writeValueAsString(Map.of("name", "g4-config-unique", "value", "other"))).status()).isEqualTo(409);
        assertThat(request("POST", CONFIG, token, null,
                JSON.writeValueAsString(Map.of("name", " ", "type", "SYSTEM"))).status()).isEqualTo(400);
        assertThat(request("POST", CONFIG, token, null,
                JSON.writeValueAsString(Map.of("name", "g4-config-invalid", "type", "UNKNOWN"))).status()).isEqualTo(400);
        assertThat(request("GET", CONFIG + "?size=201", token, null, null).status()).isEqualTo(400);
        createDictionary(token, FIRST);
        assertThat(request("POST", DICT, token, null,
                JSON.writeValueAsString(Map.of("id", SECOND, "name", "重复", "systemName", FIRST))).status()).isEqualTo(409);
        assertThat(request("POST", DICT + "/" + FIRST + "/items", token, null,
                JSON.writeValueAsString(Map.of("paramKey", "x", "paramValue", ""))).status()).isEqualTo(400);
    }

    private String createConfig(String token, String name, String value) throws Exception {
        Reply reply = request("POST", CONFIG, token, null, JSON.writeValueAsString(Map.of("name", name, "value", value)));
        assertThat(reply.status()).as(reply.raw()).isEqualTo(200);
        assertThat(reply.body().at("/data/sort").asInt()).isZero();
        assertThat(reply.body().at("/data/type").asText()).isEqualTo("SYSTEM");
        return reply.body().at("/data/id").asText();
    }

    private void createDictionary(String token, String id) throws Exception {
        Reply reply = request("POST", DICT, token, null, dictionaryCommand(id));
        assertThat(reply.status()).as(reply.raw()).isEqualTo(200);
    }

    private String createItem(String token, String basicId, String key) throws Exception {
        Reply reply = request("POST", DICT + "/" + basicId + "/items", token, null, itemCommand(key));
        assertThat(reply.status()).as(reply.raw()).isEqualTo(200);
        return reply.body().at("/data/id").asText();
    }

    private String dictionaryCommand(String id) {
        return JSON.writeValueAsString(Map.of("id", id, "name", "测试字典", "systemName", id));
    }

    private String itemCommand(String key) {
        return JSON.writeValueAsString(Map.of("paramKey", key, "paramValue", "测试字典值"));
    }
}
