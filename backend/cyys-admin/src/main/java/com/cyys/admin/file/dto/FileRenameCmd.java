package com.cyys.admin.file.dto;

import jakarta.validation.constraints.*;

public record FileRenameCmd(@NotBlank @Size(max=200) String name, @NotNull @Min(0) Integer version) {}
