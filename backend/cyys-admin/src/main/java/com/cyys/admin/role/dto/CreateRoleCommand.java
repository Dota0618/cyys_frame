package com.cyys.admin.role.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record CreateRoleCommand(@NotBlank @Size(max = 50) String name,
                     @NotNull @Min(0) @Max(1) Integer status,
                     @NotNull Integer sort,
                     @Size(max = 500) String remark,
                     @NotNull @Size(max = 500) List<@NotBlank @Size(max = 32) String> menuIds) {}
