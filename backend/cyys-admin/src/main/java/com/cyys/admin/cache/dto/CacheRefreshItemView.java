package com.cyys.admin.cache.dto;

import com.cyys.admin.cache.model.CacheRefreshItem;

public record CacheRefreshItemView(String id, String name, int version) {
    public static CacheRefreshItemView from(CacheRefreshItem row) {
        return new CacheRefreshItemView(row.getId(), row.getName(), row.getVersion());
    }
}
