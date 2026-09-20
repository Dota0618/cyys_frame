package com.cyys.admin.notice.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record CreateOrgNoticeCommand(@NotBlank @Size(max=32) String departmentId,@NotBlank @Size(max=100) String title,@Size(max=64) String type,@NotBlank @Size(max=2000) String content,@NotNull @Size(max=10) List<@NotBlank @Size(max=32) String> attachments) {}
