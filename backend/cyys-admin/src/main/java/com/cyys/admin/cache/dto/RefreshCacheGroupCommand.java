package com.cyys.admin.cache.dto;

import com.cyys.common.cache.CacheScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RefreshCacheGroupCommand(
        @NotBlank @Size(max = 50) String refreshGroup,
        @NotNull CacheScope scope
) {}
