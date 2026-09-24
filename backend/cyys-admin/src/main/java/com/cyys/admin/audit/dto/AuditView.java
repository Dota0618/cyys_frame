package com.cyys.admin.audit.dto;

import com.cyys.common.cache.CacheValue;

import java.time.LocalDateTime;

public record AuditView(String id, String userId,
                        String loginName, String realName,
                        String scopeId, String resource, String recordId, String operation,
                        @CacheValue(cache = "dictionary-item", variant = "DATA_AUDIT_OPERATION", key = "operation", field = "label") String operationLabel,
                        @CacheValue(cache = "dictionary-item", variant = "DATA_AUDIT_OPERATION", key = "operation", field = "disabled") Boolean operationDisabled,
                        String authorizedView, String outcome,
                        @CacheValue(cache = "dictionary-item", variant = "DATA_AUDIT_OUTCOME", key = "outcome", field = "label") String outcomeLabel,
                        @CacheValue(cache = "dictionary-item", variant = "DATA_AUDIT_OUTCOME", key = "outcome", field = "disabled") Boolean outcomeDisabled,
                        LocalDateTime occurredAt) {}
