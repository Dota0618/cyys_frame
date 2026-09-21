package com.cyys.admin.scope.dto;

import jakarta.validation.constraints.*;

public record CreateScopeCommand(
        @NotBlank @Pattern(regexp="[A-Za-z0-9][A-Za-z0-9_-]{0,63}") String code,
        @NotBlank @Size(max=100) String name,
        @Size(max=50) String shortName,
        @Size(max=50) String contactPerson,
        @Size(max=20) String contactPhone,
        @Size(max=500) String remark,
        @NotBlank @Size(max=32) String parentId) {}
