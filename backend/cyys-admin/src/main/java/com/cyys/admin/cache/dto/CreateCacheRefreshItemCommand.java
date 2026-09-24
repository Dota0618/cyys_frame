package com.cyys.admin.cache.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCacheRefreshItemCommand(
        @NotBlank @Size(max = 50) String name
) {}
