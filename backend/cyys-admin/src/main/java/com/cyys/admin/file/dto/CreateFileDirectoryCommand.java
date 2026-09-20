package com.cyys.admin.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateFileDirectoryCommand(@NotBlank @Size(max=100) String name) {}
