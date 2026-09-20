package com.cyys.admin.audit.dto;


import java.time.LocalDateTime;

public record AuditView(String id, String actorId, String scopeId, String resource, String recordId,
                  String operation, String authorizedView, String outcome, LocalDateTime occurredAt) {}
