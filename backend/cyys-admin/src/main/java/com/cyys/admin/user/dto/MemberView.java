package com.cyys.admin.user.dto;

import java.time.LocalDateTime;

public record MemberView(String id, String userId, String scopeId, String loginName, String displayName,
                   String orgId, int status, int version, LocalDateTime createdAt, int dataRange) {}
