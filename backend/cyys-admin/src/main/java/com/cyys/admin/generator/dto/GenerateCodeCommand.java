package com.cyys.admin.generator.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record GenerateCodeCommand(
        @NotBlank @Pattern(regexp="biz_[a-z][a-z0-9_]{0,43}") String table,
        @NotBlank @Pattern(regexp="[a-z][a-z0-9]{1,23}") String feature,
        @NotBlank @Pattern(regexp="[A-Z][A-Za-z0-9]{1,39}") String className,
        @NotBlank @Size(max=40) String title,
        @NotNull GeneratorLayout layout,
        boolean existingTable,
        @NotEmpty @Size(max=30) List<@NotNull @Valid GeneratorField> fields) {}
