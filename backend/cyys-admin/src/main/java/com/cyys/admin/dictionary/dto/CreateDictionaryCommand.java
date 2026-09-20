package com.cyys.admin.dictionary.dto;

import jakarta.validation.constraints.*;

public record CreateDictionaryCommand(
        @NotNull @Pattern(regexp = "[0-9]{1,32}", message = "字典编号应为 1–32 位数字")
        String id,
        @NotBlank @Size(max = 20)
        String name,
        @NotBlank @Size(max = 255)
        String systemName,
        Integer sort
) {}
