package com.cyys.admin.dictionary.dto;

import jakarta.validation.constraints.*;

public record UpdateDictionaryCommand(
        @NotNull
        Integer version,
        @NotBlank @Size(max = 20)
        String name,
        Integer sort
) {}
