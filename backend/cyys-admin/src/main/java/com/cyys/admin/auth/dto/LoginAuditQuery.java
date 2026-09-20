package com.cyys.admin.auth.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;


public record LoginAuditQuery(@Min(1) long page, @Min(1) @Max(200) long size,
                    @Size(max = 128) String loginName,
                    @Size(max = 20) String outcome) {}
