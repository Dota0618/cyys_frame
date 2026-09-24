package com.cyys.admin.cache.dto;

import com.cyys.common.cache.CacheScope;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCacheDefinitionCommand(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) String cacheKey,
        @NotBlank @Size(max = 50) String groupName,
        @NotBlank @Size(max = 50) String refreshGroup,
        @NotNull CacheScope scope,
        @Size(max = 255) String description,
        @NotNull @Min(0) Integer version
) {}
