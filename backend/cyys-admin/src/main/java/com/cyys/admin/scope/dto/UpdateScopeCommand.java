package com.cyys.admin.scope.dto;

import jakarta.validation.constraints.*;

public record UpdateScopeCommand(
        @NotNull @Min(0) Integer version,
        @NotBlank @Size(max=100) String name,
        @Size(max=50) String shortName,
        @Size(max=50) String contactPerson,
        @Size(max=20) String contactPhone,
        @NotNull Boolean enabled,
        @Min(0) @Max(1000000) int sort,
        @Size(max=500) String remark,
        @NotBlank @Size(max=32) String parentId) {}
