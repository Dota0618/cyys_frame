package com.cyys.admin.recycle.dto;

import com.cyys.common.cache.CacheValue;
import java.time.LocalDateTime;

public record RecycleRecord(String id, String title, String scopeId,
        @CacheValue(cache="scope", key="scopeId", field="name") String scopeName,
        String departmentId,
        @CacheValue(cache="department", key="departmentId", scopeKey="scopeId", field="name") String departmentName,
        String deletedBy,
        @CacheValue(cache="user", key="deletedBy", field="name") String deletedByName,
        LocalDateTime deletedAt, LocalDateTime createdAt, int version) {}
