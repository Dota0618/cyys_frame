package com.cyys.admin.cache.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCacheRefreshItemCommand(
        @NotBlank @Size(max = 50) String name,
        @NotNull Integer version
) {}
