package com.cyys.admin.configuration.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

public record UpdateConfigurationCommand(
        @NotNull
        Integer version,
        @JsonProperty(required = true)
        @Size(max = 100)
        String value,
        @NotNull
        ConfigurationType type,
        @Size(max = 255)
        String desp,
        Integer sort
) {}
