package com.cyys.admin.department.dto;

import jakarta.validation.constraints.*;

public record CreateDepartmentCommand(@NotBlank @Size(max = 100) String name, @Size(max = 64) String code,
                     @Size(max = 32) String parentId, @Size(max = 32) String leaderId) {}
