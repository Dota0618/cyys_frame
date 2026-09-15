package com.cyys.common.satoken;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "cyys.data-isolation", ignoreUnknownFields = false)
public record DataIsolationProperties(@NotNull Mode mode, String singleScopeId) {
    public enum Mode { SINGLE, MULTI }

    @AssertTrue(message = "single 模式必须配置真实单位主键 single-scope-id，不能使用 GLOBAL")
    public boolean isSingleScopeValid() {
        return mode != Mode.SINGLE
                || (singleScopeId != null && !singleScopeId.isBlank() && !"GLOBAL".equals(singleScopeId));
    }
}
