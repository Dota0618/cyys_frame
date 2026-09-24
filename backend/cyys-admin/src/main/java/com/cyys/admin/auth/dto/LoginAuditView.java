package com.cyys.admin.auth.dto;

import com.cyys.common.cache.CacheValue;

import java.time.LocalDateTime;

public record LoginAuditView(String id, String loginName, String realName, String userId, String scopeId,
                  String clientIp, String outcome,
                  @CacheValue(cache = "dictionary-item", variant = "LOGIN_AUDIT_OUTCOME", key = "outcome", field = "label") String outcomeLabel,
                  @CacheValue(cache = "dictionary-item", variant = "LOGIN_AUDIT_OUTCOME", key = "outcome", field = "disabled") Boolean outcomeDisabled,
                  String reason, LocalDateTime occurredAt) {}
