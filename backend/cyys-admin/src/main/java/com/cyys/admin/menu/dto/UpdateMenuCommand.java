package com.cyys.admin.menu.dto;

import jakarta.validation.constraints.*;

public record UpdateMenuCommand(@NotBlank @Size(max = 32) String id,
                   @NotNull @Min(0) Integer version,
                   @NotBlank @Size(max = 32) String parentId,
                   @NotBlank @Size(max = 50) String name,
                   @Size(max = 255) String path,
                   @Size(max = 255) String component,
                   @Size(max = 255) String redirect,
                   @Size(max = 100) String icon,
                   @NotNull Integer sort,
                   @NotNull @Min(0) @Max(1) Integer visible,
                   @NotNull @Min(0) @Max(1) Integer status,
                   @NotNull @Min(0) @Max(1) Integer platformOnly,
                   @NotNull @Min(0) @Max(1) Integer keepAlive,
                   @NotNull @Min(0) @Max(1) Integer alwaysShow) {}
