package com.cyys.admin.file.dto;

import java.time.LocalDateTime;
import java.util.List;

public record FileUploadView(String id, String name, long bytes, int partBytes, String state,
                             LocalDateTime expiresAt, List<Integer> completed, List<String> hashes) {}
