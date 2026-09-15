package com.cyys.application;

import com.cyys.admin.department.dto.DepartmentDTO;
import com.cyys.admin.department.service.DepartmentService;
import com.cyys.admin.user.service.UserDisplayService;
import com.cyys.common.satoken.ScopeContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(classes=CyysApplication.class, webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties={"cyys.data-isolation.mode=multi","cyys.data-isolation.single-scope-id="})
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class MultiScopeIsolationTest extends IsolationTestSupport {
    @Autowired DepartmentService departments;
    @Autowired UserDisplayService users;
    @Autowired PlatformTransactionManager transactions;
    @Autowired com.cyys.admin.department.mapper.SysOrgMapper orgMapper;

    @Test
    void customPaginationCountCannotBypassRegisteredMapperPolicy() {
        as("u-alice","scope-a");
        try {
            var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.cyys.admin.department.model.SysOrg>(1,2);
            page.setCountId("com.cyys.application.IsolationProbeMapper.unregisteredPolicy");
            assertThatThrownBy(() -> orgMapper.selectPage(page, new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>()))
                    .hasMessageContaining("自定义 COUNT");
        } finally { ScopeContext.clear(); }
    }

    @Test
    void queryShapesAndPageCountsUseTheSameScope() throws Exception {
        String token = token("alice");
        for (String shape : List.of("alias","left","subquery","union")) {
            var reply=ok("GET","/sql/"+shape,token,"scope-a",null);
            assertThat(reply.body().path("data").size()).as(shape).isEqualTo(3);
            assertThat(reply.raw()).doesNotContain("scope-b","g1-b-root");
        }
        assertThat(ok("GET","/sql/count",token,"scope-a",null).body().path("data").asInt()).isEqualTo(3);
        assertThat(ok("GET","/sql/group",token,"scope-a",null).body().at("/data/0/total").asInt()).isEqualTo(3);
        var scalar=ok("GET","/sql/scalar",token,"scope-a",null).body().path("data");
        scalar.forEach(row -> assertThat(row.path("total").asInt()).isEqualTo(3));
        var page=ok("GET","/departments?size=2",token,"scope-a",null).body().path("data");
        assertThat(page.path("total").asInt()).isEqualTo(3);
        assertThat(page.path("records").size()).isEqualTo(2);
        var search=ok("GET","/departments?search=root",token,"scope-a",null).body().path("data");
        assertThat(search.path("total").asInt()).isEqualTo(1);
        assertThat(search.path("records").size()).isEqualTo(1);
        assertThat(search.at("/records/0/id").asText()).isEqualTo("g1-a-root");
        assertThat(call("GET","/departments?size=201",token,"scope-a",null).status()).isEqualTo(400);
        jdbc.update("update sys_org set sort=10 where id='g1-a-self'");
        jdbc.update("update sys_org set sort=20 where id='g1-a-child'");
        jdbc.update("update sys_org set sort=30 where id='g1-a-root'");
        var ordered=ok("GET","/departments?size=2&sort=name",token,"scope-a",null).body().at("/data/records");
        assertThat(ordered.get(0).path("id").asText()).isEqualTo("g1-a-self");
        assertThat(ordered.get(1).path("id").asText()).isEqualTo("g1-a-child");
    }

    @Test
    void unknownAndUncoveredSqlAndUnsafeWritesFailClosed() throws Exception {
        String token=token("alice");
        for (String shape : List.of("policy","table","malformed","ignored","uncovered","resource","raw"))
            assertThat(call("GET","/sql/"+shape,token,"scope-a",null).status()).as(shape).isEqualTo(403);
        for (String shape : List.of("unit","owner","or"))
            assertThat(call("POST","/unsafe/"+shape,token,"scope-a",null).status()).as(shape).isEqualTo(403);
        assertThat(count("select count(*) from sys_org where name='unsafe'")).isZero();
    }

    @Test
    void headersRecordIdsAndBodyOwnershipCannotCrossScope() throws Exception {
        String token=token("alice");
        assertThat(call("GET","/departments",token,null,null).status()).isEqualTo(403);
        assertThat(call("GET","/departments",token,"scope-b",null).status()).isEqualTo(403);
        assertThat(call("GET","/departments/g1-b-root",token,"scope-a",null).status()).isEqualTo(404);
        assertThat(call("POST","/departments",token,"scope-a",Map.of("name","new","code","new","scopeId","scope-b")).status()).isEqualTo(400);
        assertThat(call("POST","/departments",token,"scope-a",Map.of("name","new","code","new","createdBy","u-bob")).status()).isEqualTo(400);
        assertThat(call("POST","/departments",token,"scope-a",Map.of("name","new","code","new","parentId","g1-b-root")).status()).isEqualTo(403);
        var made=ok("POST","/departments",token,"scope-a",Map.of("name","new","code","new")).body().path("data");
        assertThat(made.path("scopeId").asText()).isEqualTo("scope-a");
        assertThat(jdbc.queryForObject("select created_by from sys_org where id=?",String.class,made.path("id").asText())).isEqualTo("u-alice");
        assertThat(count("select count(*) from sys_data_audit where actor_id='u-alice' and scope_id='scope-a' and operation='CREATE'")).isEqualTo(1);
    }

    @Test
    void fiveRangesAreAppliedOnlyForTheCurrentOperation() throws Exception {
        String token=token("alice");
        jdbc.update("insert into sys_role_org(role_id,scope_id,org_id) values('role-a','scope-a','g1-a-child')");
        int[] expected={3,1,1,2,1};
        for (int range=1;range<=5;range++) {
            jdbc.update("update sys_role set data_range=? where id='role-a'",range);
            assertThat(ok("GET","/sql/count",token,"scope-a",null).body().path("data").asInt()).as("range "+range).isEqualTo(expected[range-1]);
        }
        role("g1-wide-read","scope-a","WIDE_READ");
        jdbc.update("update sys_role set data_range=1 where id='g1-wide-read'");
        grant("u-alice","g1-wide-read","scope-a");
        allow("g1-wide-read","department:read",false);
        assertThat(ok("GET","/sql/count",token,"scope-a",null).body().path("data").asInt()).isEqualTo(3);
        assertThat(call("PUT","/departments",token,"scope-a",Map.of("id","g1-a-root","version",0,"name","bad")).status()).isEqualTo(409);
        ok("PUT","/departments",token,"scope-a",Map.of("id","g1-a-self","version",0,"name","mine"));
        jdbc.update("update sys_role set data_range=2 where id='role-a'");
        jdbc.update("update sys_role_org set scope_id='scope-b',org_id='g1-b-root' where role_id='role-a'");
        jdbc.update("delete from sys_user_role where role_id='g1-wide-read'");
        assertThat(ok("GET","/sql/count",token,"scope-a",null).body().path("data").asInt()).isZero();
    }

    @Test
    void mixedBatchesRollBackAndVersionsPreventLostUpdates() throws Exception {
        String token=token("alice");
        assertThat(call("POST","/batch-create",token,"scope-a",List.of(Map.of("name","first","code","first"),Map.of("name","bad","code","bad","parentId","g1-b-root"))).status()).isEqualTo(403);
        assertThat(count("select count(*) from sys_org where code='first'")).isZero();
        assertThat(count("select count(*) from sys_data_audit")).isZero();
        assertThat(call("POST","/batch-delete",token,"scope-a",List.of(Map.of("id","g1-a-self","version",0),Map.of("id","g1-b-root","version",0))).status()).isEqualTo(409);
        assertThat(count("select count(*) from sys_org where deleted=1")).isZero();
        assertThat(call("POST","/batch-delete",token,"scope-a",List.of(Map.of("id","g1-a-root","version",0))).status()).isEqualTo(409);
        try (var workers=Executors.newFixedThreadPool(2)) {
            var one=workers.submit(() -> call("PUT","/departments",token,"scope-a",Map.of("id","g1-a-self","version",0,"name","one")));
            var two=workers.submit(() -> call("PUT","/departments",token,"scope-a",Map.of("id","g1-a-self","version",0,"name","two")));
            assertThat(List.of(one.get().status(),two.get().status())).containsExactlyInAnyOrder(200,409);
        }
        assertThat(count("select count(*) from sys_data_audit where operation='UPDATE'")).isEqualTo(1);
        ok("POST","/batch-delete",token,"scope-a",List.of(Map.of("id","g1-a-self","version",1)));
        assertThat(call("POST","/batch-delete",token,"scope-a",List.of(Map.of("id","g1-a-self","version",1))).status()).isEqualTo(409);
        assertThat(count("select count(*) from sys_data_audit where operation='DELETE'")).isEqualTo(1);
    }

    @Test
    void platformReadAndWriteHaveSeparateExplicitPermissions() throws Exception {
        platformUser();
        allow("role-platform","department:read",false);
        allow("role-platform","department:create",false);
        String token=token("platform");
        assertThat(call("GET","/global",token,null,null).status()).isEqualTo(403);
        allow("role-platform","platform:data:read-all",true);
        assertThat(ok("GET","/global",token,null,null).body().path("data").size()).isEqualTo(4);
        assertThat(call("POST","/departments",token,"scope-b",Map.of("name","platform","code","platform")).status()).isEqualTo(403);
        allow("role-platform","platform:data:operate",true);
        assertThat(call("POST","/departments",token,null,Map.of("name","platform","code","platform")).status()).isEqualTo(403);
        ok("POST","/departments",token,"scope-b",Map.of("name","platform","code","platform"));
        assertThat(count("select count(*) from sys_data_audit where actor_id='u-platform' and scope_id='scope-b'")).isEqualTo(1);
        jdbc.update("update sys_menu set status=0 where code='platform:data:read-all'");
        assertThat(call("GET","/global",token,null,null).status()).isEqualTo(403);
    }

    @Test
    void unitRoleGrantCannotGrantAnotherUnitPlatformOrHigherAuthority() throws Exception {
        allow("role-a","role:grant",false);
        membership("u-bob","scope-a",false);
        platformUser();
        grant("u-bob","role-platform","GLOBAL");
        String token=token("alice");
        for (String role : List.of("role-b","role-platform"))
            assertThat(call("POST","/roles/u-bob",token,"scope-a",List.of(role)).status()).isEqualTo(403);
        role("g1-higher","scope-a","HIGHER");
        allow("g1-higher","secret:write",false);
        assertThat(call("POST","/roles/u-bob",token,"scope-a",List.of("g1-higher")).status()).isEqualTo(403);
        ok("POST","/roles/u-bob",token,"scope-a",List.of("role-a"));
        assertThat(count("select count(*) from sys_user_role where user_id='u-bob' and scope_id='scope-b' and role_id='role-b'")).isEqualTo(1);
        assertThat(count("select count(*) from sys_user_role where user_id='u-bob' and scope_id='scope-a' and role_id='role-a'")).isEqualTo(1);
        assertThat(count("select count(*) from sys_user_role where user_id='u-bob' and scope_id='GLOBAL'")).isEqualTo(1);
        jdbc.update("update sys_role set data_range=5 where id='role-a'");
        assertThat(call("POST","/roles/u-bob",token,"scope-a",List.of("role-a")).status()).isEqualTo(403);
        assertThat(count("select count(*) from information_schema.referential_constraints where constraint_schema=database()")).isZero();
    }

    @Test
    void cacheHitStillChecksPermissionAndMembershipAndRefreshIsScoped() throws Exception {
        for (String role : List.of("role-a","role-b")) {
            allow(role,"user:display:read",false);
            allow(role,"cache:user:refresh",false);
        }
        String alice=token("alice"),bob=token("bob");
        assertThat(ok("GET","/user-names?ids=u-alice",alice,"scope-a",null).raw()).contains("alice");
        ok("GET","/user-names?ids=u-bob",bob,"scope-b",null);
        as("u-alice","scope-a"); String a=keys.unit("user-display"); ScopeContext.clear();
        as("u-bob","scope-b"); String b=keys.unit("user-display"); ScopeContext.clear();
        assertThat(redis.hasKey(a)).isTrue(); assertThat(redis.hasKey(b)).isTrue();
        assertThat(redis.getExpire(a)).isBetween(1L,600L);
        assertThat(call("GET","/user-names?ids=u-bob",alice,"scope-a",null).status()).isEqualTo(403);
        jdbc.update("update sys_menu set status=0 where code='user:display:read'");
        assertThat(call("GET","/user-names?ids=u-alice",alice,"scope-a",null).status()).isEqualTo(403);
        jdbc.update("update sys_menu set status=1 where code='user:display:read'");
        as("u-alice","scope-a");
        try { new TransactionTemplate(transactions).executeWithoutResult(status -> {users.refreshCurrentUnit();status.setRollbackOnly();}); }
        finally { ScopeContext.clear(); }
        assertThat(redis.hasKey(a)).isTrue();
        ok("POST","/user-cache-refresh",alice,"scope-a",null);
        assertThat(redis.hasKey(a)).isFalse(); assertThat(redis.hasKey(b)).isTrue();
        ok("GET","/user-names?ids=u-alice",alice,"scope-a",null);
        jdbc.update("update sys_user_scope set status=0 where user_id='u-alice'");
        assertThat(call("GET","/user-names?ids=u-alice",alice,"scope-a",null).status()).isEqualTo(403);
    }

    @Test
    void transactionCannotSwitchIdentityEvenAfterContextIsCleared() {
        as("u-alice","scope-a");
        try {
            assertThatThrownBy(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
                departments.detail("g1-a-self");
                ScopeContext.clear(); as("u-bob","scope-b");
                departments.detail("g1-b-root");
            })).hasMessageContaining("同一事务不得切换");
        } finally { ScopeContext.clear(); }
        as("u-bob","scope-b");
        try { assertThat(departments.detail("g1-b-root").scopeId()).isEqualTo("scope-b"); }
        finally { ScopeContext.clear(); }
    }

    @Test
    void reusedWorkerRequiresExplicitIdentityAndClearsAfterFailure() throws Exception {
        try (var worker=Executors.newSingleThreadExecutor()) {
            worker.submit(() -> {
                as("u-alice","scope-a");
                try { assertThatThrownBy(() -> departments.detail("g1-b-root")).hasMessageContaining("不可访问"); }
                finally { ScopeContext.clear(); }
            }).get();
            worker.submit(() -> assertThatThrownBy(() -> departments.detail("g1-a-self"))
                    .hasRootCauseInstanceOf(IllegalStateException.class).hasMessageContaining("上下文尚未建立")).get();
            worker.submit(() -> {
                as("u-bob","scope-b");
                try { assertThat(departments.detail("g1-b-root").scopeId()).isEqualTo("scope-b"); }
                finally { ScopeContext.clear(); }
            }).get();
        }
    }
}
