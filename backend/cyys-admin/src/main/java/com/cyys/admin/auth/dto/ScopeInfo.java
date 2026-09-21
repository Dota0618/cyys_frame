package com.cyys.admin.auth.dto;

import com.cyys.common.cache.CacheValue;

/** defaultFlag 表示所属单位；可选单位和 parentId 由实时数据范围查询提供。 */
public record ScopeInfo(String id, String code,
                        @CacheValue(cache="scope", key="id", field="name") String name,
                        boolean defaultFlag, String parentId) {}
