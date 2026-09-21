package com.cyys.common.cache;

import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;

/** 有界 Hash 按需回源；区域代次阻止失效前开始的查询再次填入旧值。 */
@Component
@RequiredArgsConstructor
public final class ScopedHashCache {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ScopedHashCache.class);
    private static final int MAX_ENTRIES = 10_000;
    private static final int MAX_VALUE_BYTES = 256 * 1024;
    private static final long TTL_SECONDS = Duration.ofMinutes(10).toSeconds();
    private static final String GENERATION = "@generation";
    private static final DefaultRedisScript<String> OPEN_SCRIPT = script("redis/bounded-hash-open.lua", String.class);
    private static final DefaultRedisScript<Long> FILL_SCRIPT = script("redis/bounded-hash-fill.lua", Long.class);
    private static final DefaultRedisScript<Long> EVICT_FIELDS_SCRIPT = script("redis/bounded-hash-evict-fields.lua", Long.class);
    private final StringRedisTemplate redis;
    private final CacheKeys keys;

    public Map<String, String> getOrLoad(CacheRegion region, List<String> fields,
                                       Function<List<String>, Map<String, String>> loader) {
        return getOrLoad(region, region.scope() == CacheScope.UNIT
                ? com.cyys.common.satoken.ScopeContext.get().requireScopeId() : null, fields, loader);
    }

    public Map<String, String> getOrLoad(CacheRegion region, String scopeId, List<String> fields,
                                       Function<List<String>, Map<String, String>> loader) {
        if (fields.isEmpty()) return Map.of();
        var unique = fields.stream().distinct().toList();
        if (unique.contains(GENERATION)) throw new IllegalArgumentException("保留缓存字段");
        // 写事务可能读到自身未提交修改；未知/RR 隔离级别也可能回填旧事务快照。
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && (!TransactionSynchronizationManager.isCurrentTransactionReadOnly()
                || !Integer.valueOf(java.sql.Connection.TRANSACTION_READ_COMMITTED).equals(
                    TransactionSynchronizationManager.getCurrentTransactionIsolationLevel())))
            return Map.copyOf(loader.apply(unique));
        String key = region.scope() == CacheScope.UNIT ? keys.unit(scopeId, region.id()) : keys.platform(region.id());
        String generation = null;
        Map<String, String> result = new LinkedHashMap<>();
        try {
            generation = redis.execute(OPEN_SCRIPT, List.of(key), UUID.randomUUID().toString(), Long.toString(TTL_SECONDS));
            List<String> cached = redis.<String, String>opsForHash().multiGet(key, unique);
            if (cached != null) for (int i = 0; i < unique.size(); i++) {
                if (cached.get(i) != null) result.put(unique.get(i), cached.get(i));
            }
        } catch (DataAccessException unavailable) {
            generation = null;
            LOG.warn("Cache read unavailable; falling back to source, region={}", region.id());
        }
        List<String> missing = unique.stream().filter(field -> !result.containsKey(field)).toList();
        if (!missing.isEmpty()) {
            Map<String, String> loaded = loader.apply(missing);
            result.putAll(loaded);
            if (generation != null && !loaded.isEmpty()) fill(key, generation, loaded, region.id());
        }
        return Map.copyOf(result);
    }

    private void fill(String key, String generation, Map<String, String> values, String region) {
        var arguments = new ArrayList<String>();
        arguments.add(Integer.toString(MAX_ENTRIES));
        arguments.add(generation);
        values.forEach((field, value) -> {
            if (!GENERATION.equals(field) && value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= MAX_VALUE_BYTES) {
                arguments.add(field); arguments.add(value);
            }
        });
        if (arguments.size() == 2) return;
        try {
            redis.execute(FILL_SCRIPT, List.of(key), arguments.toArray());
        } catch (DataAccessException unavailable) {
            LOG.warn("Cache fill unavailable; continuing without cache, region={}", region);
        }
    }

    List<String> groupKeys(CacheGroup group) { return group.regions().stream().map(this::key).distinct().toList(); }
    private String key(CacheRegion region) {
        return region.scope() == CacheScope.UNIT ? keys.unit(region.id()) : keys.platform(region.id());
    }

    void evict(List<String> groupKeys, boolean required) {
        evict(groupKeys, List.of(), required);
    }

    void evict(List<String> groupKeys, List<String> fields, boolean required) {
        try {
            // 单次 DEL 覆盖固定组内键；不扫描，不触及会话或其他单位。
            Long result;
            if (fields.isEmpty()) result = redis.delete(groupKeys);
            else {
                var arguments = new ArrayList<String>(); arguments.add(UUID.randomUUID().toString()); arguments.addAll(fields);
                result = redis.execute(EVICT_FIELDS_SCRIPT, groupKeys, arguments.toArray());
            }
            if (result == null) throw new org.springframework.data.redis.RedisSystemException("Missing eviction result", null);
        } catch (DataAccessException unavailable) {
            if (required) throw new com.cyys.common.web.ApiException(503, "缓存服务暂不可用，请稍后重试");
            LOG.warn("Cache group eviction unavailable; entries remain bounded by TTL, regions={}", groupKeys.size());
        }
    }

    private static <T> DefaultRedisScript<T> script(String path, Class<T> type) {
        var script = new DefaultRedisScript<T>();
        script.setLocation(new ClassPathResource(path)); script.setResultType(type); return script;
    }
}
