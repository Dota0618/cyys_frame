package com.cyys.admin.user.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record UpdateMemberDataPermissionCommand(
        @NotNull @Min(0) Integer version,
        @NotNull @Min(0) @Max(6) Integer dataRange,
        @NotNull @Size(max = 500) List<@NotBlank @Size(max = 32) String> orgIds) {}
