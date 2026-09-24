package com.cyys.application;

import com.cyys.admin.cache.service.BusinessCacheGroups;
import com.cyys.admin.scope.dto.UpdateScopeCommand;
import com.cyys.admin.scope.mapper.ScopeDisplayMapper;
import com.cyys.admin.scope.service.ScopeCacheSource;
import com.cyys.admin.scope.service.ScopeManagementService;
import com.cyys.common.cache.CacheKeys;
import com.cyys.common.satoken.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@SpringBootTest(classes=CyysApplication.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties="cyys.environment=scope-cache-acceptance")
class ScopeCacheIntegrationTest extends HttpTestSupport {
    @MockitoSpyBean ScopeDisplayMapper display;
    @Autowired ScopeCacheSource source;
    @Autowired ScopeManagementService scopes;
    @Autowired CacheKeys keys;
    @Autowired StringRedisTemplate redis;
    @Autowired TransactionTemplate tx;

    @BeforeEach void prepareScopeCache() {
        clearCaches();
        jdbc.update("update sys_scope set name='总单位 A',version=0 where id='scope-a'");
        permission("role-a","g1-cache-scope","platform:scope:update",false);
        permission("role-a","g1-cache-refresh","cache:user:refresh",false);
        clearInvocations(display);
    }
    @AfterEach void cleanupScopeCache() {
        ScopeContext.clear();
        clearCaches();
        jdbc.update("delete from sys_data_audit where scope_id='GLOBAL' and "
                + "((resource='scope' and record_id='scope-a') or (resource='cache' and operation='REFRESH'))");
    }
    private void clearCaches() {
        redis.delete(List.of(keys.platform(BusinessCacheGroups.SCOPE_NAMES.id()),
                keys.platform(BusinessCacheGroups.USER_NAMES.id())));
    }

    @Test void loginAndHeaderNamesShareCacheWhileDisabledUnitsStayOutOfChoices() throws Exception {
        String token=token("alice");
        var me=request("GET","/api/auth/me",token,"scope-a",null);
        assertThat(me.status()).as(me.raw()).isEqualTo(200);
        assertThat(me.body().at("/data/scopes/0/name").asText()).isEqualTo("总单位 A");
        assertThat(me.body().at("/data/defaultScope/name").asText()).isEqualTo("总单位 A");
        verify(display,times(1)).labels(List.of("scope-a"));
        var disabled=source.records(null,"",List.of("scope-disabled"));
        assertThat(disabled.get("scope-disabled").name()).isEqualTo("已禁用单位");
        assertThat(source.records(null,"",List.of("missing"))).isEmpty();
        assertThat(me.raw()).doesNotContain("scope-disabled");
    }

    @Test void scopeUpdateInvalidatesOnlyCommittedUnitAndKeepsOtherRecords() throws Exception {
        String token=token("alice");
        source.records(null,"",List.of("scope-b"));
        String key=keys.platform(BusinessCacheGroups.SCOPE_NAMES.id());
        ScopeContext.set(new RequestIdentity("u-alice","scope-a",List.of(),List.of("platform:scope:update")));
        try {
            tx.executeWithoutResult(status -> {
                scopes.update("scope-a",new UpdateScopeCommand(0,"回滚单位",null,null,null,true,0,
                        "CYYS automated test fixture","0"));
                assertThat(redis.opsForHash().hasKey(key,"scope-a")).isTrue();
                status.setRollbackOnly();
            });
        } finally { ScopeContext.clear(); }
        assertThat(source.records(null,"",List.of("scope-a")).get("scope-a").name()).isEqualTo("总单位 A");
        var updated=request("PUT","/api/admin/platform/scopes/scope-a",token,"scope-a",JSON.writeValueAsString(Map.of(
                "version",0,"name","改名单位","enabled",true,"sort",0,"parentId","0","remark","CYYS automated test fixture")));
        assertThat(updated.status()).as(updated.raw()).isEqualTo(200);
        assertThat(redis.opsForHash().hasKey(key,"scope-a")).isFalse();
        assertThat(redis.opsForHash().hasKey(key,"scope-b")).isTrue();
        var me=request("GET","/api/auth/me",token,"scope-a",null);
        assertThat(me.body().at("/data/scopes/0/name").asText()).isEqualTo("改名单位");
        verify(display,times(2)).labels(List.of("scope-a"));
    }

    @Test void manualScopeRefreshUsesRegisteredGroupAndKeepsUserCacheAndLogin() throws Exception {
        String token=token("alice");
        var actions=request("GET","/api/admin/caches",token,"scope-a",null);
        assertThat(actions.status()).as(actions.raw()).isEqualTo(200);
        assertThat(actions.raw()).contains("\"groupName\":\"单位\"", "\"cacheKey\":\"scope-records\"");
        String userKey=keys.platform(BusinessCacheGroups.USER_NAMES.id());
        redis.opsForHash().put(userKey,"user-fixture","preserved");
        var refreshed=request("POST","/api/admin/caches/refresh-groups/refresh",token,"scope-a",
                JSON.writeValueAsString(Map.of("refreshGroup", "单位缓存", "scope", "SYSTEM")));
        assertThat(refreshed.status()).as(refreshed.raw()).isEqualTo(200);
        assertThat(redis.hasKey(keys.platform(BusinessCacheGroups.SCOPE_NAMES.id()))).isFalse();
        assertThat(redis.opsForHash().get(userKey,"user-fixture")).isEqualTo("preserved");
        assertThat(request("GET","/api/auth/me",token,"scope-a",null).status()).isEqualTo(200);
    }
}
