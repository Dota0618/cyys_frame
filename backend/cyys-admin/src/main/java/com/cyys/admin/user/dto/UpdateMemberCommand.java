package com.cyys.admin.user.dto;

import jakarta.validation.constraints.*;

public record UpdateMemberCommand(@NotNull @Min(0) Integer version, @NotBlank @Size(max=100) String displayName,
                   @Size(max=32) String orgId, @NotNull @Min(0) @Max(1) Integer status) {}
