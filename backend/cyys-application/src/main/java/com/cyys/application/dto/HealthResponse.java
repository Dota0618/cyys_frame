package com.cyys.application.dto;

import java.time.LocalDateTime;

public record HealthResponse(String status, String service, String version, LocalDateTime timestamp) {}
