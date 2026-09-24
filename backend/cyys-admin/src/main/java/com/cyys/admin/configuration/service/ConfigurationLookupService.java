package com.cyys.admin.configuration.service;

import com.cyys.admin.cache.service.BusinessCacheGroups;
import com.cyys.admin.configuration.mapper.ConfigureMapper;
import com.cyys.common.cache.ScopedHashCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 读取全局参数；缓存未命中或 Redis 不可用时回源数据库。 */
@Service
@RequiredArgsConstructor
public class ConfigurationLookupService {
    private final ConfigureMapper mapper;
    private final ScopedHashCache cache;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String value(String name) {
        return values(List.of(name)).get(name);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Map<String, String> publicValues() {
        return values(ConfigurationKeys.PUBLIC_KEYS);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Map<String, String> values(Collection<String> names) {
        List<String> requested = names.stream().filter(name -> name != null && !name.isBlank()).distinct().toList();
        if (requested.isEmpty()) return Map.of();
        return cache.getOrLoad(BusinessCacheGroups.CONFIGURATION_VALUES, requested, missing ->
                mapper.values(missing).stream()
                        .filter(row -> row.getValue() != null)
                        .collect(LinkedHashMap::new,
                                (result, row) -> result.put(row.getName(), row.getValue()),
                                Map::putAll));
    }
}
