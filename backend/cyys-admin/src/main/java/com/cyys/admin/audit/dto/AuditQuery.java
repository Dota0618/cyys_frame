package com.cyys.admin.audit.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;


public record AuditQuery(@Min(1) long page, @Min(1) @Max(200) long size,
                    @Size(max = 64) String resource,
                    @Size(max = 20) String operation) {}
