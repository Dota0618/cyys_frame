package com.cyys.admin.auth.dto;


import java.time.LocalDateTime;

public record LoginAuditView(String id, String loginName, String userId, String scopeId,
                  String clientIp, String outcome, String reason,
                  LocalDateTime occurredAt) {}
