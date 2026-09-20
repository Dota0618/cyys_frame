package com.cyys.admin.region.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateRegionCommand(
        @Size(max=32) String parentId,
        @NotBlank @Size(max=20) String cityCode,
        @NotBlank @Size(max=20) String name,
        @Size(max=20) String shortName,
        @Size(max=6) String zipCode,
        @DecimalMin("-180") @DecimalMax("180") @Digits(integer=3,fraction=15) BigDecimal longitude,
        @DecimalMin("-90") @DecimalMax("90") @Digits(integer=2,fraction=15) BigDecimal latitude,
        Integer sort) {}
