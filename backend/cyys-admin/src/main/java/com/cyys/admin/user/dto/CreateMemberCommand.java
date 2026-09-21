package com.cyys.admin.user.dto;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;

public record CreateMemberCommand(@NotBlank @Pattern(regexp="[A-Za-z0-9][A-Za-z0-9_.-]{2,63}") String loginName,
                     @NotBlank @Size(min=12,max=72) String password,
                     @NotBlank @Size(max=100) String displayName, @Size(max=32) String orgId,
                     @Valid MemberDataPermissionInput dataPermission) {}
