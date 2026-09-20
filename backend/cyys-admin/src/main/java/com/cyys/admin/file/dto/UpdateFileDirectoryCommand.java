package com.cyys.admin.file.dto;

import jakarta.validation.constraints.*;

public record UpdateFileDirectoryCommand(@NotBlank @Size(max=100) String name, @NotNull @Min(0) Integer version) {}
