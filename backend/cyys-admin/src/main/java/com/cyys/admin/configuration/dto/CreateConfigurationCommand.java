package com.cyys.admin.configuration.dto;

import jakarta.validation.constraints.*;

public record CreateConfigurationCommand(
        @NotBlank @Size(max = 50)
        String name,
        @Size(max = 100)
        String value,
        ConfigurationType type,
        @Size(max = 255)
        String desp,
        Integer sort
) {}
