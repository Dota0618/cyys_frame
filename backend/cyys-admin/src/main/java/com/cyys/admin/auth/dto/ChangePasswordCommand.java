package com.cyys.admin.auth.dto;

import jakarta.validation.constraints.*;

public record ChangePasswordCommand(@NotBlank @Size(max=72) String currentPassword,
                                    @NotBlank @Size(min=8,max=72) String newPassword) {}
