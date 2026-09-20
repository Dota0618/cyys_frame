package com.cyys.admin.role.dto;

import jakarta.validation.constraints.*;

public record PlatformRoleQuery(@Min(1) long page, @Min(1) @Max(200) long size,
                    @Size(max = 100) String search) {}
