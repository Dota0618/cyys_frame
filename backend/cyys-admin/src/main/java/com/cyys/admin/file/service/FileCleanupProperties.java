package com.cyys.admin.file.service;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** 生命周期维护的部署参数，不开放业务页面修改。 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix="cyys.file-cleanup", ignoreUnknownFields=false)
public class FileCleanupProperties {
    private boolean enabled = true;
    @Min(1) @Max(100) private int batchSize = 25;
    @Min(1000) private long intervalMs = 60000;
    @Min(30) @Max(86400) private int retrySeconds = 300;
}
