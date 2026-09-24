package com.cyys.admin.cache.model;

import lombok.Data;

@Data
public class CacheDefinition {
    private String id;
    private String name;
    private String cacheKey;
    private String groupName;
    private String refreshGroup;
    private String scope;
    private String description;
    private Integer version = 0;
}
