package com.cyys.admin.dictionary.dto;

import jakarta.validation.constraints.*;

public record CreateDictionaryItemCommand(
        @NotBlank @Size(max = 20)
        String paramKey,
        @NotBlank @Size(max = 20)
        String paramValue,
        @Size(max = 200)
        String desp,
        Integer sort
) {}
