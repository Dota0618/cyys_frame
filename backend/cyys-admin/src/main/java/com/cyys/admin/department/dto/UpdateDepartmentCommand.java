package com.cyys.admin.department.dto;

import jakarta.validation.constraints.*;

public record UpdateDepartmentCommand(@NotBlank @Size(max = 32) String id, @NotNull @Min(0) Integer version,
                   @NotBlank @Size(max = 100) String name, @Size(max = 32) String leaderId) {}
