package com.cyys.admin.recycle.dto;

import jakarta.validation.constraints.*;

public record UpdateRecycleConfigurationCommand(
        @NotNull Integer version,
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Pattern(regexp = "[a-z][a-z0-9_]{0,63}") String titleColumn,
        @NotBlank @Pattern(regexp = "[a-z][a-z0-9_]{0,63}") String searchColumn,
        @NotNull Boolean scopeRequired,
        @Pattern(regexp = "[a-z][a-z0-9_]{0,63}") String scopeColumn,
        @Pattern(regexp = "[a-z][a-z0-9_]{0,63}") String departmentColumn,
        @Pattern(regexp = "[a-z][a-z0-9_]{0,63}") String ancestorsColumn,
        @Pattern(regexp = "[a-z][a-z0-9_]{0,63}") String actorColumn,
        @Pattern(regexp = "[a-z][a-z0-9_]{0,63}") String versionColumn,
        @Pattern(regexp = "[a-z][a-z0-9_]{0,63}") String deletedColumn,
        @Pattern(regexp = "[a-z][a-z0-9_]{0,63}") String createdAtColumn,
        @Pattern(regexp = "[a-z][a-z0-9_]{0,63}") String updatedAtColumn,
        @Pattern(regexp = "[a-z][a-z0-9_]{0,63}") String updatedByColumn,
        @Min(-2147483648) @Max(2147483647) Integer sort,
        @NotNull Boolean enabled) {}
