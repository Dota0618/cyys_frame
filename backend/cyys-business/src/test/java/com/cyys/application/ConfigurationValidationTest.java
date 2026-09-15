package com.cyys.application;

import com.cyys.common.satoken.DataIsolationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationValidationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(PropertiesConfig.class);

    @Test
    void invalidModeMissingUnitAndProtectionBypassSettingsFailBinding() {
        runner.withPropertyValues("cyys.data-isolation.mode=misspelled")
                .run(context -> assertThat(context).hasFailed());
        runner.withPropertyValues("cyys.data-isolation.mode=single")
                .run(context -> assertThat(context).hasFailed());
        runner.withPropertyValues("cyys.data-isolation.mode=single", "cyys.data-isolation.single-scope-id=GLOBAL")
                .run(context -> assertThat(context).hasFailed());
        runner.withPropertyValues("cyys.data-isolation.mode=multi", "cyys.data-isolation.enabled=false")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void validModesBindWithoutDisablingProtection() {
        runner.withPropertyValues("cyys.data-isolation.mode=single", "cyys.data-isolation.single-scope-id=scope-a")
                .run(context -> assertThat(context).hasNotFailed());
        runner.withPropertyValues("cyys.data-isolation.mode=multi")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(DataIsolationProperties.class)
    static class PropertiesConfig {
    }
}
