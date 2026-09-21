package com.cyys.admin.file.dto;

import jakarta.validation.constraints.*;
import com.cyys.common.validation.SafeFileName;

public record FileRenameCmd(@SafeFileName String name, @NotNull @Min(0) Integer version) {}
