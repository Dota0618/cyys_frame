package com.cyys.common.cache;

import java.util.List;

/** 一个业务组可包含多个同范围的实际缓存区域。 */
public record CacheGroup(String id, List<CacheRegion> regions) {
    public CacheGroup {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("缓存组不能为空");
        regions = List.copyOf(regions);
        if (regions.isEmpty() || regions.stream().map(CacheRegion::scope).distinct().count() != 1)
            throw new IllegalArgumentException("缓存组必须包含同范围的缓存区域");
    }
    public CacheScope scope() { return regions.getFirst().scope(); }
}
