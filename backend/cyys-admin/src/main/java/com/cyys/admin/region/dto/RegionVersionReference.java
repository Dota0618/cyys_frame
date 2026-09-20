package com.cyys.admin.region.dto;

import jakarta.validation.constraints.*;

public record RegionVersionReference(@NotBlank @Size(max=32) String id,@NotNull @Min(0) Integer version) {}
