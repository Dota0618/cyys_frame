package com.cyys.admin.auth.service;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import lombok.Data;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "cyys.auth", ignoreUnknownFields = false)
public class LoginProperties {
    @Min(1) @Max(100)
    private int loginMaxErrorCount = 5;
    @Min(1) @Max(1440)
    private int lockMinutes = 15;
}
