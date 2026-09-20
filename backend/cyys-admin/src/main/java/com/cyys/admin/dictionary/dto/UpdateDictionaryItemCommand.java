package com.cyys.admin.dictionary.dto;

import jakarta.validation.constraints.*;

public record UpdateDictionaryItemCommand(
        @NotNull
        Integer version,
        @NotBlank @Size(max = 20)
        String paramValue,
        @Size(max = 200)
        String desp,
        Integer sort
) {}
