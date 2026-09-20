package com.cyys.admin.notice.dto;

import jakarta.validation.constraints.*;

public record CreateSystemNoticeCommand(
        @NotBlank @Size(max=100) String title,
        @NotBlank @Size(max=20) String type,
        @NotBlank @Size(max=2000) String content,
        boolean visible) {}
