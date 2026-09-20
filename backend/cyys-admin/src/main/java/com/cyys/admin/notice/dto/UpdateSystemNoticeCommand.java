package com.cyys.admin.notice.dto;

import jakarta.validation.constraints.*;

public record UpdateSystemNoticeCommand(
        @NotNull @Min(0) Integer version,
        @NotBlank @Size(max=100) String title,
        @NotBlank @Size(max=20) String type,
        @NotBlank @Size(max=2000) String content,
        @NotNull Boolean visible) {}
