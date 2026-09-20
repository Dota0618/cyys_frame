package com.cyys.admin.file.dto;

import jakarta.validation.constraints.*;

public record FileVersionReference(@NotBlank @Size(max=32) String id, @NotNull @Min(0) Integer version) {}
