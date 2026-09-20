package com.cyys.admin.user.service;

import com.cyys.admin.auth.mapper.DataAuthorizationMapper;
import com.cyys.admin.user.mapper.UserDisplayMapper;
import com.cyys.common.cache.ScopedHashCache;
import com.cyys.common.mybatis.DataPolicyRegistry;
import com.cyys.common.mybatis.DataTransactionGuard;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 只缓存单位内用户显示名称；每次命中前仍核验当前授权和目标成员。 */
@Service
@Validated
@RequiredArgsConstructor
public class UserDisplayService {
    private static final String CACHE_REGION = "user-display";
    private final UserDisplayMapper mapper;
    private final DataAuthorizationMapper authorization;
    private final ScopedHashCache cache;

    public Map<String,String> names(@NotEmpty @Size(max=100) List<@NotBlank String> ids) {
        require("user:display:read");
        List<String> unique = ids.stream().distinct().toList();
        if (mapper.visibleMembers(ScopeContext.get().requireScopeId(), unique).size() != unique.size()) {
            throw new ApiException(403, "请求包含不可访问的单位成员");
        }
        Map<String,String> values = new LinkedHashMap<>();
        values.putAll(cache.getUnit(CACHE_REGION, unique));
        List<String> missing = unique.stream().filter(id -> !values.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            Map<String,String> loaded = new LinkedHashMap<>();
            mapper.labels(ScopeContext.get().requireScopeId(),missing).forEach(label -> loaded.put(label.id(), label.name()));
            values.putAll(loaded);
            if (!loaded.isEmpty()) {
                cache.putUnit(CACHE_REGION, loaded);
            }
        }
        return Map.copyOf(values);
    }

    public void refreshCurrentUnit() {
        require("cache:user:refresh");
        cache.refreshUnitAfterCommit(CACHE_REGION);
    }

    @org.springframework.transaction.annotation.Transactional(propagation=org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void invalidateAfterCommit() {
        cache.evictUnitAfterCommit(CACHE_REGION);
    }

    private void require(String permission) {
        var identity = ScopeContext.get();
        DataTransactionGuard.bind(identity, DataPolicyRegistry.View.CURRENT_SCOPE);
        if (!authorization.hasPermission(identity.loginId(), identity.requireScopeId(), permission)) {
            throw new ApiException(403, "没有当前单位的缓存数据访问权限");
        }
    }
}
