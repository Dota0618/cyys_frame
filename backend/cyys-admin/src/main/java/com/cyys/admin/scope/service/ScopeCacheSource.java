package com.cyys.admin.scope.service;

import com.cyys.admin.cache.service.BusinessCacheGroups;
import com.cyys.admin.scope.dto.ScopeLabel;
import com.cyys.admin.scope.mapper.ScopeDisplayMapper;
import com.cyys.common.cache.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ScopeCacheSource implements CacheValueSource<ScopeLabel> {
    private final RecordCache cache;
    private final ScopeDisplayMapper mapper;
    public String name() { return "scope"; }
    public CacheScope scope() { return CacheScope.SYSTEM; }
    public Class<ScopeLabel> recordType() { return ScopeLabel.class; }
    public Map<String,ScopeLabel> records(String scopeId, String variant, List<String> ids) {
        if (!variant.isEmpty()) return Map.of();
        return cache.getOrLoad(BusinessCacheGroups.SCOPE_NAMES, null, ids, ScopeLabel.class,
                missing -> mapper.labels(missing).stream().collect(Collectors.toMap(ScopeLabel::id, Function.identity())));
    }
}
