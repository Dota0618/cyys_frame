package com.cyys.admin.cache.dto;

import com.cyys.admin.cache.model.CacheDefinition;

public record CacheDefinitionView(String id, String name, String cacheKey, String groupName,
                                  String refreshGroup, String scope, String description, int version) {
    public static CacheDefinitionView from(CacheDefinition row) {
        return new CacheDefinitionView(row.getId(), row.getName(), row.getCacheKey(), row.getGroupName(),
                row.getRefreshGroup(), row.getScope(), row.getDescription(), row.getVersion());
    }
}
