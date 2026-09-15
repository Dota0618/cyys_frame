package com.cyys.application;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(initializers = MySqlTestDatabaseGuard.class)
@Import(MySqlTestFixtures.class)
abstract class HttpTestSupport {
    static final String PASSWORD = "correct-password";
    static final String PASSWORD_HASH = BCrypt.hashpw(PASSWORD, BCrypt.gensalt(4));
    static final JsonMapper JSON = JsonMapper.builder().build();
    static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @Autowired JdbcTemplate jdbc;
    @Value("${local.server.port}") int port;

    @BeforeEach
    void fixtures() {
        cleanupFixtures();
        user("u-alice", "alice");
        user("u-bob", "bob");
        membership("u-alice", "scope-a", true);
        membership("u-bob", "scope-b", true);
        role("role-a", "scope-a", "A_READER");
        role("role-b", "scope-b", "B_WRITER");
        grant("u-alice", "role-a", "scope-a");
        grant("u-bob", "role-b", "scope-b");
        permission("role-a", "permission-read", "test:read", false);
        permission("role-b", "permission-write", "test:write", false);
    }

    @AfterEach
    void cleanupFixtures() {
        for (String id : jdbc.queryForList("select id from sys_user where login_name like 'g4-%'",String.class)) {
            StpUtil.logout(id);
            jdbc.update("delete from sys_user_role where user_id=?",id);
            jdbc.update("delete from sys_user_scope where user_id=?",id);
            jdbc.update("delete from sys_user where id=?",id);
        }
        for (String id : List.of("u-alice", "u-bob", "u-platform", "1")) {
            StpUtil.logout(id);
        }
        // 启动前已确认这是空的测试 schema；只清理由本组用例插入的记录。
        jdbc.update("delete from sys_data_audit where scope_id in ('scope-a','scope-b')");
        jdbc.update("delete from sys_role_org where scope_id in ('scope-a','scope-b')");
        jdbc.update("delete from sys_role_menu where role_id in ('role-a','role-b','role-platform')");
        jdbc.update("delete from sys_role_menu where role_id like 'g1-%'");
        jdbc.update("delete from sys_user_role where user_id in ('u-alice','u-bob','u-platform','1')");
        jdbc.update("delete from sys_user_scope where user_id in ('u-alice','u-bob','u-platform','1')");
        jdbc.update("delete from sys_role where id in ('role-a','role-b','role-platform')");
        jdbc.update("delete from sys_role where id like 'g1-%'");
        jdbc.update("delete from sys_user where id in ('u-alice','u-bob','u-platform','1')");
        jdbc.update("delete from sys_menu where id in ('permission-read','permission-write','platform-access','bad')");
        jdbc.update("delete from sys_menu where id like 'g1-%'");
        jdbc.update("delete from sys_org where scope_id in ('scope-a','scope-b')");
        jdbc.update("update sys_scope set status=1, deleted=0 where id in ('scope-a','scope-b')");
    }

    @AfterAll
    static void cleanupScopeFixtures(@Autowired JdbcTemplate jdbc) {
        jdbc.update("delete from sys_scope where id in ('scope-a','scope-b','scope-disabled') "
                + "and remark='CYYS automated test fixture'");
    }

    void user(String id, String name) {
        jdbc.update("insert into sys_user(id,login_name,password_hash,real_name,id_card) values(?,?,?,?,?)",
                id, name, PASSWORD_HASH, name, "private-id-card");
    }

    void membership(String userId, String scopeId, boolean preferred) {
        jdbc.update("insert into sys_user_scope(id,user_id,scope_id,default_flag) values(?,?,?,?)",
                userId + "-" + scopeId, userId, scopeId, preferred ? 1 : 0);
    }

    void role(String id, String scopeId, String code) {
        jdbc.update("insert into sys_role(id,scope_id,code,name) values(?,?,?,?)", id, scopeId, code, code);
    }

    void grant(String userId, String roleId, String scopeId) {
        jdbc.update("insert into sys_user_role(id,user_id,role_id,scope_id) values(?,?,?,?)",
                userId + "-" + roleId, userId, roleId, scopeId);
    }

    void permission(String roleId, String id, String code, boolean platformOnly) {
        jdbc.update("insert into sys_menu(id,code,name,type,platform_only) values(?,?,?,3,?)",
                id, code, code, platformOnly ? 1 : 0);
        jdbc.update("insert into sys_role_menu(id,role_id,menu_id) values(?,?,?)",
                com.baomidou.mybatisplus.core.toolkit.IdWorker.getIdStr(), roleId, id);
    }

    void platformUser() {
        user("u-platform", "platform");
        role("role-platform", "GLOBAL", "PLATFORM_OPERATOR");
        grant("u-platform", "role-platform", "GLOBAL");
        permission("role-platform", "platform-access", "platform:scope:access", true);
        jdbc.update("insert into sys_role_menu(id,role_id,menu_id) values('platform-read','role-platform','permission-read')");
    }

    Reply login(String username, String password, String scopeId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("loginName", username);
        body.put("password", password);
        if (scopeId != null) {
            body.put("scopeId", scopeId);
        }
        return request("POST", "/api/auth/login", null, null, JSON.writeValueAsString(body));
    }

    String token(String username) throws Exception {
        Reply result = login(username, PASSWORD, null);
        assertThat(result.status()).as(result.raw()).isEqualTo(200);
        return result.body().path("data").path("token").asText();
    }

    Reply request(String method, String path, String token, String scopeId, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(15));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (scopeId != null) {
            builder.header("X-Cyys-Scope-Id", scopeId);
        }
        if (body != null) {
            builder.header("Content-Type", "application/json");
        }
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body));
        return send(builder.build());
    }

    Reply send(HttpRequest request) throws Exception {
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), JSON.readTree(response.body()), response.body(),
                response.headers().allValues("Set-Cookie"));
    }

    record Reply(int status, JsonNode body, String raw, List<String> cookies) {
    }
}
