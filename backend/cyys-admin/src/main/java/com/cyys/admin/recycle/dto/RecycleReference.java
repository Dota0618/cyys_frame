package com.cyys.admin.recycle.dto;

import jakarta.validation.constraints.*;

public record RecycleReference(@NotBlank @Size(max=32) String id, @NotNull @Min(0) Integer version) {}
