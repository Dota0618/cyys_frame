package com.cyys.admin.department.dto;

import jakarta.validation.constraints.*;

public record DepartmentVersionedId(@NotBlank @Size(max = 32) String id, @NotNull @Min(0) Integer version) {}
