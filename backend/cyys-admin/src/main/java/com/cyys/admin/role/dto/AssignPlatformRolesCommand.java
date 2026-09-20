package com.cyys.admin.role.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record AssignPlatformRolesCommand(@NotNull @Size(max = 200) List<@NotBlank @Size(max = 32) String> roleIds) {}
