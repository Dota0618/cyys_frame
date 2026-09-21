package com.cyys.admin.user.dto;

import java.time.LocalDateTime;
import com.cyys.common.cache.CacheValue;

public record MemberView(String id, String userId, String scopeId, String loginName, String displayName,
                   String orgId, int status, int version, LocalDateTime createdAt, int dataRange,
                   @CacheValue(cache="scope", key="scopeId", field="name") String scopeName,
                   @CacheValue(cache="department", key="orgId", scopeKey="scopeId", field="name") String orgName) {}
