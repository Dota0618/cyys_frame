package com.cyys.application;

import com.cyys.common.cache.CacheGroupService;
import com.cyys.common.cache.CacheKeys;
import com.cyys.common.cache.ScopedHashCache;
import com.cyys.common.satoken.RequestIdentity;
import com.cyys.common.satoken.ScopeContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import static com.cyys.admin.cache.service.BusinessCacheGroups.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CacheInvalidationServiceTest {
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final CacheKeys keys = new CacheKeys("invalidation-test", "local");
    private final CacheGroupService groups = new CacheGroupService(new ScopedHashCache(redis, keys));
    private final Connection database = mock(Connection.class);
    private TransactionTemplate transactions;

    @BeforeEach void setup() throws Exception {
        var source = mock(DataSource.class);
        when(source.getConnection()).thenReturn(database);
        when(database.getAutoCommit()).thenReturn(true);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(source));
        when(redis.delete(anyList())).thenReturn(1L);
        when(redis.execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(), anyList(), any(Object[].class)))
                .thenReturn(1L);
        as("a");
    }

    @AfterEach void clear() { ScopeContext.clear(); }

    @Test void overlappingEntryRequestsProduceOneEvictionAfterCommit() throws Exception {
        transactions.executeWithoutResult(status -> {
            var ids = new ArrayList<>(List.of("b", "a", "a"));
            groups.invalidateEntriesAfterCommit(USERS, ids);
            ids.clear();
            groups.invalidateEntriesAfterCommit(USERS, List.of("c", "b"));
            verifyNoInteractions(redis);
        });
        verify(database).commit();
        verify(redis).execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                eq(List.of(keys.platform(USER_NAMES.id()))),
                anyString(), eq("a"), eq("b"), eq("c"));
        verifyNoMoreInteractions(redis);
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void wholeGroupInvalidationSupersedesEntriesInEitherOrder(boolean wholeFirst) {
        transactions.executeWithoutResult(status -> {
            if (wholeFirst) groups.invalidateAfterCommit(USERS);
            groups.invalidateEntriesAfterCommit(USERS, List.of("a"));
            groups.invalidateAfterCommit(USERS);
            groups.invalidateEntriesAfterCommit(USERS, List.of("b"));
            verifyNoInteractions(redis);
        });
        verify(redis).delete(List.of(keys.platform(USER_NAMES.id())));
        verifyNoMoreInteractions(redis);
    }

    @Test void rollbackAndFailedCommitDoNotEvictMergedRequests() throws Exception {
        transactions.executeWithoutResult(status -> {
            groups.invalidateEntriesAfterCommit(USERS, List.of("a"));
            groups.invalidateAfterCommit(USERS);
            status.setRollbackOnly();
        });
        verify(database).rollback();
        doThrow(new java.sql.SQLException("commit failed")).when(database).commit();
        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> {
            groups.invalidateEntriesAfterCommit(USERS, List.of("a"));
            groups.invalidateEntriesAfterCommit(USERS, List.of("b"));
        })).isInstanceOf(org.springframework.transaction.TransactionException.class);
        verifyNoInteractions(redis);
    }

    @Test void differentScopesAndGroupsRetainTheirCapturedKeys() {
        String scopeA = keys.unit(DEPARTMENT_NAMES.id());
        transactions.executeWithoutResult(status -> {
            groups.invalidateAfterCommit(DEPARTMENTS);
            as("b");
            groups.invalidateAfterCommit(DEPARTMENTS);
            groups.invalidateAfterCommit(USERS);
            as("c");
        });
        verify(redis).delete(List.of(scopeA));
        as("b");
        verify(redis).delete(List.of(keys.unit(DEPARTMENT_NAMES.id())));
        verify(redis).delete(List.of(keys.platform(USER_NAMES.id())));
        verifyNoMoreInteractions(redis);
    }

    @Test void separateTransactionsDoNotSharePendingInvalidations() {
        transactions.executeWithoutResult(status -> groups.invalidateAfterCommit(USERS));
        transactions.executeWithoutResult(status -> groups.invalidateEntriesAfterCommit(USERS, List.of("a")));
        verify(redis).delete(List.of(keys.platform(USER_NAMES.id())));
        verify(redis).execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                eq(List.of(keys.platform(USER_NAMES.id()))),
                anyString(), eq("a"));
        verifyNoMoreInteractions(redis);
    }

    private void as(String scope) {
        ScopeContext.clear();
        ScopeContext.set(new RequestIdentity("actor", scope, List.of(), List.of()));
    }
}
