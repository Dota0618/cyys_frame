package com.cyys.admin.notice.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record UpdateOrgNoticeCommand(@NotBlank @Size(max=100) String title,@Size(max=64) String type,@NotBlank @Size(max=2000) String content,@NotNull @Size(max=10) List<@NotBlank @Size(max=32) String> attachments,@NotNull @Min(0) Integer version) {}
