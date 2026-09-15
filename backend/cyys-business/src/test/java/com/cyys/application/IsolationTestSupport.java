package com.cyys.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cyys.common.cache.CacheKeys;
import com.cyys.common.satoken.RequestIdentity;
import com.cyys.common.satoken.ScopeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

abstract class IsolationTestSupport extends HttpTestSupport {
    static final String BASE = "/api/test/isolation";
    @Autowired StringRedisTemplate redis;
    @Autowired CacheKeys keys;

    @BeforeEach
    void isolationFixtures() {
        for (String action : List.of("read","create","update","delete")) {
            allow("role-a", "department:" + action, false);
            allow("role-b", "department:" + action, false);
        }
        jdbc.update("update sys_role set data_range=1 where id in ('role-a','role-b')");
        org("g1-a-root", "scope-a", null, "", "u-bob");
        org("g1-a-child", "scope-a", "g1-a-root", "g1-a-root", "u-bob");
        org("g1-a-self", "scope-a", null, "", "u-alice");
        org("g1-b-root", "scope-b", null, "", "u-bob");
        jdbc.update("update sys_user_scope set org_id='g1-a-root' where user_id='u-alice' and scope_id='scope-a'");
        clearTestCaches();
    }

    @AfterEach
    void clearTestCaches() {
        ScopeContext.clear();
        try {
            for (String scope : List.of("scope-a", "scope-b")) {
                as("u-alice", scope);
                redis.delete(keys.unit("user-display"));
                ScopeContext.clear();
            }
        } finally { ScopeContext.clear(); }
    }

    void org(String id, String scope, String parent, String ancestors, String creator) {
        jdbc.update("insert into sys_org(id,scope_id,parent_id,ancestors,name,code,created_by) values(?,?,?,?,?,?,?)",
                id,scope,parent,ancestors,id,id,creator);
    }

    void allow(String role, String code, boolean platform) {
        List<String> existing = jdbc.queryForList("select id from sys_menu where code=?", String.class, code);
        String id;
        if (existing.isEmpty()) {
            id = "g1-" + IdWorker.getIdStr();
            permission(role, id, code, platform);
        } else {
            id = existing.getFirst();
            jdbc.update("insert into sys_role_menu(id,role_id,menu_id) values(?,?,?)", IdWorker.getIdStr(),role,id);
        }
    }

    void as(String user, String scope) {
        ScopeContext.set(new RequestIdentity(user,scope,List.of(),List.of(),List.of()));
    }

    Reply call(String method, String path, String token, String scope, Object body) throws Exception {
        return request(method, BASE + path, token, scope, body == null ? null : JSON.writeValueAsString(body));
    }

    Reply ok(String method, String path, String token, String scope, Object body) throws Exception {
        Reply reply = call(method,path,token,scope,body);
        assertThat(reply.status()).as(reply.raw()).isEqualTo(200);
        return reply;
    }

    int count(String sql) { return jdbc.queryForObject(sql,Integer.class); }
}
