package com.cyys.common.cache;

import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 单位范围的有界 Hash 缓存；Redis 不可用时统一降级为未命中。 */
@Component
@RequiredArgsConstructor
public final class ScopedHashCache {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ScopedHashCache.class);
    private static final int MAX_ENTRIES = 10_000;
    private static final long TTL_SECONDS = Duration.ofMinutes(10).toSeconds();
    private static final DefaultRedisScript<Long> FILL_SCRIPT = fillScript();
    private final StringRedisTemplate redis;
    private final CacheKeys keys;

    public Map<String, String> getUnit(String region, List<String> fields) {
        if (fields.isEmpty()) return Map.of();
        try {
            List<String> cached = redis.<String, String>opsForHash().multiGet(keys.unit(region), fields);
            if (cached == null) return Map.of();
            Map<String, String> result = new LinkedHashMap<>();
            for (int index = 0; index < fields.size(); index++) {
                String value = cached.get(index);
                if (value != null) result.put(fields.get(index), value);
            }
            return Map.copyOf(result);
        } catch (DataAccessException unavailable) {
            LOG.warn("Cache read unavailable; falling back to source, region={}", region);
            return Map.of();
        }
    }

    public void putUnit(String region, Map<String, String> values) {
        if (values.isEmpty()) return;
        var arguments = new ArrayList<String>(2 + values.size() * 2);
        arguments.add(Integer.toString(MAX_ENTRIES));
        arguments.add(Long.toString(TTL_SECONDS));
        values.forEach((field, value) -> {
            arguments.add(field);
            arguments.add(value);
        });
        try {
            Long stored = redis.execute(FILL_SCRIPT, List.of(keys.unit(region)), arguments.toArray());
            if (!Long.valueOf(1).equals(stored)) {
                LOG.warn("Cache region capacity reached; skipping fill, region={}", region);
            }
        } catch (DataAccessException unavailable) {
            LOG.warn("Cache fill unavailable; continuing without cache, region={}", region);
        }
    }

    public void evictUnit(String region) {
        evict(keys.unit(region), region, false);
    }

    private void evict(String key, String region, boolean required) {
        try {
            redis.delete(key);
        } catch (DataAccessException unavailable) {
            if (required) {
                throw new com.cyys.common.web.ApiException(503, "缓存服务暂不可用，请稍后重试");
            }
            LOG.warn("Cache eviction unavailable; entry remains bounded by TTL, region={}", region);
        }
    }

    public void evictUnitAfterCommit(String region) {
        evictAfterCommit(region, false);
    }

    /** 显式管理操作必须报告 Redis 失败，不能将后台失效的容错语义作为成功响应。 */
    public void refreshUnitAfterCommit(String region) {
        evictAfterCommit(region, true);
    }

    private void evictAfterCommit(String region, boolean required) {
        String key = keys.unit(region);
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            evict(key, region, required);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                evict(key, region, required);
            }
        });
    }

    private static DefaultRedisScript<Long> fillScript() {
        var script = new DefaultRedisScript<Long>();
        script.setLocation(new ClassPathResource("redis/bounded-hash-fill.lua"));
        script.setResultType(Long.class);
        return script;
    }
}
