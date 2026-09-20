package com.cyys.admin.file.dto;

public record FileUploadLimitsDTO(long directBytes, long resumableBytes, int partBytes) {}
