package com.cyys.admin.auth.service;

import com.cyys.admin.configuration.service.ConfigurationKeys;
import com.cyys.admin.configuration.service.ConfigurationLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 动态读取密码强度和登录锁定策略。 */
@Service
@RequiredArgsConstructor
public class SystemSecuritySettings {
    private final ConfigurationLookupService configurations;

    public PasswordSettings current() {
        return new PasswordSettings(
                boundedInt(configurations.value(ConfigurationKeys.PWD_STRENGTH), 0, 0, 2),
                boundedInt(configurations.value(ConfigurationKeys.PWD_LOCK_NUM), 5, 0, Integer.MAX_VALUE),
                boundedInt(configurations.value(ConfigurationKeys.PWD_LOCK_HOUR), 1, 1, Integer.MAX_VALUE));
    }

    private static int boundedInt(String value, int fallback, int min, int max) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(value.strip())));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public record PasswordSettings(int strength, int lockAttempts, int lockHours) {}
}
