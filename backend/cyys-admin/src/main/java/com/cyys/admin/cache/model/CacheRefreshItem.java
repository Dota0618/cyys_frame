package com.cyys.admin.cache.model;

import lombok.Data;

@Data
public class CacheRefreshItem {
    private String id;
    private String name;
    private Integer version = 0;
}
