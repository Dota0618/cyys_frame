package com.cyys.application;

import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.auth.mapper.DataAuthorizationMapper;
import com.cyys.admin.auth.service.PlatformSecurityService;
import com.cyys.admin.cache.service.*;
import com.cyys.admin.dictionary.dto.*;
import com.cyys.admin.dictionary.mapper.*;
import com.cyys.admin.dictionary.model.*;
import com.cyys.admin.dictionary.service.*;
import com.cyys.admin.user.dto.*;
import com.cyys.admin.user.mapper.*;
import com.cyys.admin.user.model.SysUserScope;
import com.cyys.admin.user.service.*;
import com.cyys.common.cache.*;
import com.cyys.common.mybatis.*;
import com.cyys.common.satoken.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.*;
import tools.jackson.databind.json.JsonMapper;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.cyys.admin.cache.service.BusinessCacheGroups.*;

/** 使用真实 Redis/Lua、Spring JDBC 事务同步；数据库连接模拟提交/回滚，不接触保留业务库。 */
class CacheGroupBehaviorTest {
    static LettuceConnectionFactory connectionFactory;
    StringRedisTemplate redis;
    CacheKeys keys;
    ScopedHashCache cache;
    CacheGroupService groups;
    TransactionTemplate tx;
    Connection database;
    final List<String> ownedKeys = new ArrayList<>();

    @BeforeAll static void connect() {
        var configuration = new RedisStandaloneConfiguration("127.0.0.1",
                Integer.parseInt(System.getenv().getOrDefault("CYYS_CACHE_TEST_REDIS_PORT", "6379")));
        String password = System.getenv("CYYS_TEST_REDIS_PASSWORD");
        if (password != null && !password.isBlank()) configuration.setPassword(password);
        connectionFactory = new LettuceConnectionFactory(configuration);
        connectionFactory.afterPropertiesSet();
    }
    @AfterAll static void disconnect() { if (connectionFactory != null) connectionFactory.destroy(); }

    @BeforeEach void setup() throws Exception {
        redis = spy(new StringRedisTemplate(connectionFactory));
        keys = new CacheKeys("cache-verification", UUID.randomUUID().toString());
        cache = new ScopedHashCache(redis, keys);
        groups = new CacheGroupService(cache);
        database = mock(Connection.class);
        when(database.getAutoCommit()).thenReturn(true);
        var source = mock(DataSource.class);
        when(source.getConnection()).thenReturn(database);
        tx = new TransactionTemplate(new DataSourceTransactionManager(source));
        as("a"); ownedKeys.add(keys.unit(DEPARTMENT_NAMES.id()));
        as("b"); ownedKeys.add(keys.unit(DEPARTMENT_NAMES.id()));
        ownedKeys.add(keys.platform(DICTIONARY_OPTIONS.id()));
        ownedKeys.add(keys.platform(DICTIONARY_LABELS.id()));
        ownedKeys.add(keys.platform(BusinessCacheGroups.USER_NAMES.id()));
        ownedKeys.add(keys.sessionPrefix() + "sentinel");
        as("a");
    }
    @AfterEach void clean() { ScopeContext.clear(); redis.delete(ownedKeys); }
    void as(String scope) { ScopeContext.clear(); ScopeContext.set(new RequestIdentity("actor", scope, List.of(), List.of())); }
    void seed() {
        as("a"); cache.getOrLoad(DEPARTMENT_NAMES, List.of("u"), ids -> Map.of("u", "A"));
        as("b"); cache.getOrLoad(DEPARTMENT_NAMES, List.of("u"), ids -> Map.of("u", "B"));
        as("a");
        cache.getOrLoad(DICTIONARY_OPTIONS, List.of("D"), ids -> Map.of("D", "[]"));
        cache.getOrLoad(DICTIONARY_LABELS, List.of("D"), ids -> Map.of("D", "label"));
        cache.getOrLoad(BusinessCacheGroups.USER_NAMES, List.of("u"), ids -> Map.of("u", "cached-user"));
        redis.opsForValue().set(ownedKeys.getLast(), "session", java.time.Duration.ofMinutes(2));
    }

    @Test void commitCapturesScopeCoalescesAndLeavesOtherGroupsAndSessions() throws Exception {
        seed();
        tx.executeWithoutResult(status -> {
            groups.invalidateAfterCommit(DEPARTMENTS);
            groups.invalidateAfterCommit(DEPARTMENTS);
            assertThat(redis.hasKey(ownedKeys.getFirst())).isTrue();
            as("b");
        });
        verify(database).commit();
        verify(redis, times(1)).delete(List.of(ownedKeys.getFirst()));
        assertThat(redis.hasKey(ownedKeys.getFirst())).isFalse();
        for (String key : ownedKeys.subList(1, ownedKeys.size())) assertThat(redis.hasKey(key)).isTrue();
    }

    @Test void rollbackAndFailedDatabaseCommitNeverEvict() throws Exception {
        seed();
        tx.executeWithoutResult(status -> { groups.invalidateAfterCommit(DICTIONARIES); status.setRollbackOnly(); });
        verify(database).rollback();
        assertThat(redis.hasKey(keys.platform(DICTIONARY_OPTIONS.id()))).isTrue();
        doThrow(new java.sql.SQLException("commit failed")).when(database).commit();
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> groups.invalidateAfterCommit(DICTIONARIES)))
                .isInstanceOf(org.springframework.transaction.TransactionException.class);
        assertThat(redis.hasKey(keys.platform(DICTIONARY_LABELS.id()))).isTrue();
    }

    @Test void manualDictionaryRefreshIsAtomicScopedAndRequiresDictionaryPermission() {
        seed();
        var authorization = mock(DataAuthorizationMapper.class);
        when(authorization.hasPermission("actor", "cache:user:refresh")).thenReturn(true);
        var audit = mock(DataAuditService.class);
        var management = new CacheManagementService(groups, authorization, audit, tx);
        assertThat(management.actions()).hasSize(1);
        assertThatThrownBy(() -> management.refresh("dictionary")).hasMessageContaining("字典");
        assertThatThrownBy(() -> management.refresh("flushall")).hasMessageContaining("不存在");
        assertThat(redis.hasKey(keys.platform(DICTIONARY_OPTIONS.id()))).isTrue();
        when(authorization.hasPermission("actor", "dictionary:update")).thenReturn(true);
        assertThat(management.actions()).hasSize(2);
        management.refresh("dictionary");
        assertThat(redis.hasKey(keys.platform(DICTIONARY_OPTIONS.id()))).isFalse();
        assertThat(redis.hasKey(keys.platform(DICTIONARY_LABELS.id()))).isFalse();
        assertThat(redis.hasKey(ownedKeys.getFirst())).isTrue();
        assertThat(redis.opsForValue().get(ownedKeys.getLast())).isEqualTo("session");
        verify(audit).platformSuccess("cache", "dictionary", "REFRESH");
    }

    @Test void delayedLoadCannotRefillInvalidatedRegionAndTtlIsBounded() {
        var result = cache.getOrLoad(DEPARTMENT_NAMES, List.of("u"), ids -> {
            groups.refresh(DEPARTMENTS); // 等价于另一个请求在查询期间完成刷新。
            return Map.of("u", "old");
        });
        assertThat(result).containsEntry("u", "old");
        assertThat(redis.hasKey(keys.unit(DEPARTMENT_NAMES.id()))).isFalse();
        assertThat(cache.getOrLoad(DEPARTMENT_NAMES, List.of("u"), ids -> Map.of("u", "new"))).containsEntry("u", "new");
        assertThat(redis.getExpire(keys.unit(DEPARTMENT_NAMES.id()))).isBetween(1L, 600L);
    }

    @Test void writeTransactionDoesNotReadOrPopulateCache() {
        seed();
        tx.executeWithoutResult(status -> {
            assertThat(cache.getOrLoad(DEPARTMENT_NAMES, List.of("u"), ids -> Map.of("u", "uncommitted")))
                    .containsEntry("u", "uncommitted");
            status.setRollbackOnly();
        });
        assertThat(cache.getOrLoad(DEPARTMENT_NAMES, List.of("u"), ids -> { throw new AssertionError(); })).containsEntry("u", "A");
    }

    @Test void targetedCommitBlocksStaleFillAndPreservesOtherUsersAndTtl() {
        var region = BusinessCacheGroups.USER_NAMES;
        String key = keys.platform(region.id());
        cache.getOrLoad(region, List.of("other"), ids -> Map.of("other", "保留"));
        redis.expire(key, java.time.Duration.ofSeconds(90));
        var inFlight = cache.getOrLoad(region, List.of("changed"), ids -> {
            tx.executeWithoutResult(status -> groups.invalidateEntriesAfterCommit(BusinessCacheGroups.USERS, List.of("changed")));
            return Map.of("changed", "旧姓名");
        });
        assertThat(inFlight).containsEntry("changed", "旧姓名");
        assertThat(redis.opsForHash().hasKey(key, "changed")).isFalse();
        assertThat(redis.opsForHash().get(key, "other")).isEqualTo("保留");
        assertThat(redis.getExpire(key)).isBetween(1L, 90L);
        assertThat(cache.getOrLoad(region, List.of("changed"), ids -> Map.of("changed", "新姓名")))
                .containsEntry("changed", "新姓名");
    }

    @Test void dictionaryOptionsAndLabelsAreSharedHitsButEligibilityAlwaysUsesDatabase() {
        var mapper = mock(DictionaryOptionMapper.class);
        var option = new DictionaryOption("1", "名称", false);
        when(mapper.options("D")).thenReturn(List.of(option));
        when(mapper.labels("D", List.of("1"))).thenReturn(List.of(option));
        var lookup = new DictionaryLookupService(mapper, cache, JsonMapper.builder().build());
        assertThat(lookup.options("D")).containsExactly(option);
        assertThat(lookup.labels("D", List.of("1"))).containsExactly(option);
        as("b");
        assertThat(lookup.options("D")).containsExactly(option);
        assertThat(lookup.labels("D", List.of("1"))).containsExactly(option);
        verify(mapper, times(1)).options("D"); verify(mapper, times(1)).labels("D", List.of("1"));
        assertThat(lookup.eligible("D", "1")).isNull();
        verify(mapper).eligible("D", "1");
        groups.refresh(DICTIONARIES);
        when(mapper.options("D")).thenReturn(List.of());
        assertThat(lookup.options("D")).isEmpty();
        assertThat(lookup.options("D")).isEmpty();
        verify(mapper, times(2)).options("D");
    }

    @ParameterizedTest @ValueSource(strings = {"create", "update", "delete", "createItem", "updateItem", "deleteItem"})
    void allDictionaryWritesEvictBothRegionsOnlyAfterCommit(String operation) {
        var mapper = mock(DictionaryMapper.class);
        var dictionary = new ParamBasic(); dictionary.setId("D"); dictionary.setName("分类"); dictionary.setSort(0); dictionary.setVersion(0);
        var item = new ParamBasicType(); item.setId("i"); item.setBasicId("D"); item.setParamKey("1"); item.setVersion(0);
        when(mapper.lock("D")).thenReturn(dictionary); when(mapper.item("D", "i")).thenReturn(item);
        when(mapper.create(any(), anyString())).thenReturn(1); when(mapper.update(any(), anyString())).thenReturn(1);
        when(mapper.softDelete(anyString(), anyInt(), anyString())).thenReturn(1);
        when(mapper.createItem(any(), anyString())).thenReturn(1); when(mapper.updateItem(any(), anyString())).thenReturn(1);
        when(mapper.deleteItem(anyString(), anyString(), anyInt(), anyString())).thenReturn(1);
        var service = new DictionaryService(mapper, mock(DataAuditService.class), groups);
        for (boolean rollback : List.of(true, false)) {
            seed();
            tx.executeWithoutResult(status -> {
                switch (operation) {
                    case "create" -> service.create(new CreateDictionaryCommand("D", "分类", "D", 0));
                    case "update" -> service.update("D", new UpdateDictionaryCommand(0, "新分类", 0));
                    case "delete" -> service.delete("D", 0);
                    case "createItem" -> service.createItem("D", new CreateDictionaryItemCommand("1", "新名称", null, 0));
                    case "updateItem" -> service.updateItem("D", "i", new UpdateDictionaryItemCommand(0, "新名称", null, 0));
                    case "deleteItem" -> service.deleteItem("D", "i", 0);
                    default -> throw new AssertionError();
                }
                assertThat(redis.hasKey(keys.platform(DICTIONARY_OPTIONS.id()))).isTrue();
                if (rollback) status.setRollbackOnly();
            });
            assertThat(redis.hasKey(keys.platform(DICTIONARY_OPTIONS.id()))).isEqualTo(rollback);
            assertThat(redis.hasKey(keys.platform(DICTIONARY_LABELS.id()))).isEqualTo(rollback);
            assertThat(redis.hasKey(keys.unit(DEPARTMENT_NAMES.id()))).isTrue();
        }
    }

    @ParameterizedTest @ValueSource(strings = {"create", "edit"})
    void memberWritesInvalidateTheirBusinessGroup(String operation) {
        var mapper = mock(MemberMapper.class); var accounts = mock(MemberAccountMapper.class);
        var row = new SysUserScope(); row.setId("m"); row.setUserId("u"); row.setScopeId("a"); row.setVersion(0); row.setDataRange(5);
        when(mapper.insert(any(SysUserScope.class))).thenReturn(1);
        when(mapper.lockForEdit("m")).thenReturn(row); when(mapper.updateById(any(SysUserScope.class))).thenReturn(1);
        when(accounts.accountNames(anyList())).thenReturn(List.of(new MemberOption("u", "login")));
        var service = new MemberService(mapper, accounts, mock(MemberDepartmentMapper.class), mock(com.cyys.admin.role.service.RoleGrantService.class), mock(DataAuditService.class),
                groups, mock(MemberDataPermissionService.class), mock(PlatformSecurityService.class));
        for (boolean rollback : List.of(true, false)) {
            seed();
            tx.executeWithoutResult(status -> {
                if (operation.equals("create")) service.create(new CreateMemberCommand("cache-user", "Cache-Test-12345!", "新用户", null, null));
                else service.edit("m", new UpdateMemberCommand(0, "新姓名", null, 1, null));
                assertThat(redis.hasKey(keys.unit(DEPARTMENT_NAMES.id()))).isTrue();
                if (rollback) status.setRollbackOnly();
            });
            if (operation.equals("edit")) assertThat(redis.opsForHash().hasKey(keys.platform(BusinessCacheGroups.USER_NAMES.id()), "u")).isEqualTo(rollback);
            assertThat(redis.hasKey(keys.unit(DEPARTMENT_NAMES.id()))).isTrue();
            assertThat(redis.hasKey(keys.platform(DICTIONARY_OPTIONS.id()))).isTrue();
        }
    }

    @Test void missingTransactionFailsVisibly() {
        assertThatThrownBy(() -> groups.invalidateAfterCommit(DEPARTMENTS)).isInstanceOf(IllegalStateException.class);
    }

    @Test void redisFailuresFallBackOnReadAndDoNotUndoCommittedBusinessWrites() throws Exception {
        seed();
        var offline = mock(StringRedisTemplate.class);
        when(offline.execute(any(org.springframework.data.redis.core.script.RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new org.springframework.data.redis.RedisConnectionFailureException("offline"));
        var unavailable = new ScopedHashCache(offline, keys);
        assertThat(unavailable.getOrLoad(DEPARTMENT_NAMES, List.of("u"), ids -> Map.of("u", "source")))
                .containsEntry("u", "source");
        doThrow(new org.springframework.data.redis.RedisConnectionFailureException("offline"))
                .when(redis).delete(List.of(keys.unit(DEPARTMENT_NAMES.id())));
        assertThatCode(() -> tx.executeWithoutResult(status -> groups.invalidateAfterCommit(DEPARTMENTS))).doesNotThrowAnyException();
        verify(database).commit();
        assertThat(redis.getExpire(keys.unit(DEPARTMENT_NAMES.id()))).isBetween(1L, 600L);
    }

    @Test void capacityAndOversizeValuesReturnSourceWithoutGrowingCache() {
        var fields = java.util.stream.IntStream.range(0, 10_001).mapToObj(Integer::toString).toList();
        Map<String, String> loaded = new HashMap<>(); fields.forEach(field -> loaded.put(field, "value"));
        assertThat(cache.getOrLoad(DEPARTMENT_NAMES, fields, missing -> loaded)).hasSize(10_001);
        assertThat(redis.opsForHash().size(keys.unit(DEPARTMENT_NAMES.id()))).isEqualTo(1); // 只有代次元数据
        String large = "大".repeat(100_000);
        assertThat(cache.getOrLoad(DEPARTMENT_NAMES, List.of("large"), missing -> Map.of("large", large))).containsEntry("large", large);
        assertThat(redis.opsForHash().hasKey(keys.unit(DEPARTMENT_NAMES.id()), "large")).isFalse();
    }

}
