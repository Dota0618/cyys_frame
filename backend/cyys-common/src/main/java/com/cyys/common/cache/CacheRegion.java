package com.cyys.common.cache;

import java.util.Objects;

/** 由代码声明的区域，业务范围不能由客户端指定。 */
public record CacheRegion(String id, CacheScope scope) {
    public CacheRegion {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("缓存区域不能为空");
        Objects.requireNonNull(scope);
    }
}
