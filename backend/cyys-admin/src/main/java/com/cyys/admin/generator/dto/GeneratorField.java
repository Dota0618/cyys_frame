package com.cyys.admin.generator.dto;

import jakarta.validation.constraints.*;

public record GeneratorField(
        @NotBlank @Pattern(regexp="[a-z][a-z0-9_]{0,39}") String column,
        @NotBlank @Size(max=40) String label,
        @NotNull GeneratorFieldType type,
        boolean required, boolean inList, boolean searchable,
        @Min(1) @Max(2000) Integer length,
        @Size(max=64) String dictionary) {}
