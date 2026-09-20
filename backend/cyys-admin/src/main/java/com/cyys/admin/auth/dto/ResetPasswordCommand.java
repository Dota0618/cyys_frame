package com.cyys.admin.auth.dto;

import jakarta.validation.constraints.*;

public record ResetPasswordCommand(@NotBlank @Size(min=12,max=72) String newPassword) {}
