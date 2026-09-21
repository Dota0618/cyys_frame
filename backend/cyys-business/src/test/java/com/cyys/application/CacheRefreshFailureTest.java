package com.cyys.application;

import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.auth.mapper.DataAuthorizationMapper;
import com.cyys.admin.cache.service.CacheManagementService;
import com.cyys.common.cache.CacheKeys;
import com.cyys.common.cache.ScopedHashCache;
import com.cyys.common.satoken.RequestIdentity;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CacheRefreshFailureTest {
    @AfterEach void clear() { ScopeContext.clear(); }

    @Test
    void explicitRefreshReportsUnavailableRedisAndNeverRecordsFalseSuccess() {
        ScopeContext.set(new RequestIdentity("actor", "scope-a", List.of(), List.of()));
        var redis = mock(StringRedisTemplate.class);
        var keys = new CacheKeys("test", "local");
        when(redis.delete(List.of(keys.unit("user-display")))).thenThrow(new RedisConnectionFailureException("offline"));
        var cache = new ScopedHashCache(redis, keys);
        var authorization = mock(DataAuthorizationMapper.class);
        when(authorization.hasPermission("actor", "cache:user:refresh")).thenReturn(true);
        var audit = mock(DataAuditService.class);
        var transactions = mock(TransactionTemplate.class);
        var service = new CacheManagementService(new com.cyys.common.cache.CacheGroupService(cache), authorization, audit, transactions);
        assertThatThrownBy(() -> service.refresh("user-display"))
                .isInstanceOfSatisfying(ApiException.class, error -> org.assertj.core.api.Assertions.assertThat(error.getStatus()).isEqualTo(503));
        verifyNoInteractions(audit, transactions);
    }
}
