package com.cyys.admin.user.service;

import com.cyys.admin.auth.mapper.DataAuthorizationMapper;
import com.cyys.admin.user.mapper.UserDisplayMapper;
import com.cyys.common.cache.CacheKeys;
import com.cyys.common.mybatis.DataAccess;
import com.cyys.common.mybatis.DataTransactionGuard;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.annotation.Validated;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 只缓存单位内用户显示名称；每次命中前仍核验当前授权和目标成员。 */
@Service
@Validated
@RequiredArgsConstructor
public class UserDisplayService {
    private final UserDisplayMapper mapper;
    private final DataAuthorizationMapper authorization;
    private final StringRedisTemplate redis;
    private final CacheKeys keys;
    private static final DefaultRedisScript<Long> FILL = new DefaultRedisScript<>("""
            if redis.call('HLEN', KEYS[1]) + (#ARGV - 2) / 2 > tonumber(ARGV[1]) then return 0 end
            redis.call('HSET', KEYS[1], unpack(ARGV, 3))
            if redis.call('TTL', KEYS[1]) < 0 then redis.call('EXPIRE', KEYS[1], ARGV[2]) end
            return 1
            """, Long.class);

    public Map<String,String> names(@NotEmpty @Size(max=100) List<@NotBlank String> ids) {
        require("user:display:read");
        List<String> unique = ids.stream().distinct().toList();
        if (mapper.visibleMembers(ScopeContext.get().requireScopeId(), unique).size() != unique.size()) {
            throw new ApiException(403, "请求包含不可访问的单位成员");
        }
        String key = keys.unit("user-display");
        Map<String,String> values = new LinkedHashMap<>();
        try {
            var cached = redis.<String,String>opsForHash().multiGet(key, unique);
            for (int index=0; index<unique.size(); index++) if (cached.get(index) != null) values.put(unique.get(index), cached.get(index));
        } catch (DataAccessException unavailable) {
            // 展示缓存不可用时，已通过授权的请求回源；不改变权限结论。
        }
        List<String> missing = unique.stream().filter(id -> !values.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            Map<String,String> loaded = new LinkedHashMap<>();
            mapper.labels(ScopeContext.get().requireScopeId(),missing).forEach(label -> loaded.put(label.id(), label.name()));
            values.putAll(loaded);
            if (!loaded.isEmpty()) {
                try {
                    var arguments = new java.util.ArrayList<String>(List.of("10000", "600"));
                    loaded.forEach((id, name) -> { arguments.add(id); arguments.add(name); });
                    // 容量检查、写入及首次过期原子执行；后续填充不延长已有名称的有效期。
                    redis.execute(FILL, List.of(key), arguments.toArray());
                } catch (DataAccessException unavailable) {
                    // 返回已查询的展示值，不将缓存写入当作业务写入。
                }
            }
        }
        return Map.copyOf(values);
    }

    public void refreshCurrentUnit() {
        require("cache:user:refresh");
        String key = keys.unit("user-display");
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { redis.delete(key); }
            });
        } else {
            redis.delete(key);
        }
    }

    @org.springframework.transaction.annotation.Transactional(propagation=org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void invalidateAfterCommit() {
        String key=keys.unit("user-display");
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                try {redis.delete(key);}
                catch(DataAccessException error) {
                    org.slf4j.LoggerFactory.getLogger(UserDisplayService.class).warn("Member name cache eviction failed; bounded by TTL, key={}",key);
                }
            }
        });
    }

    private void require(String permission) {
        var identity = ScopeContext.get();
        DataTransactionGuard.bind(identity, DataAccess.View.CURRENT_SCOPE);
        if (!authorization.hasPermission(identity.loginId(), identity.requireScopeId(), permission)) {
            throw new ApiException(403, "没有当前单位的缓存数据访问权限");
        }
    }
}
