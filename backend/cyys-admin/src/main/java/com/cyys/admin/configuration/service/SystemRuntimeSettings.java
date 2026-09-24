package com.cyys.admin.configuration.service;

import com.cyys.common.export.BoundedXlsx;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 读取全局运行参数；参数缺失或非法时使用安全默认值。 */
@Service
@RequiredArgsConstructor
public class SystemRuntimeSettings {
    private static final long MB = 1024L * 1024L;
    private final ConfigurationLookupService configurations;

    public RuntimeSettings current() {
        return new RuntimeSettings(
                text(ConfigurationKeys.COPYRIGHT_TEXT),
                text(ConfigurationKeys.CONTACT_PHONE),
                flag(ConfigurationKeys.MAINTENANCE_MODE),
                boundedInt(ConfigurationKeys.PASSWORD_EXPIRE_DAYS, 0, 0, Integer.MAX_VALUE),
                megabytes(ConfigurationKeys.FILE_MAX_SIZE_MB),
                boundedInt(ConfigurationKeys.EXPORT_MAX_ROWS, BoundedXlsx.MAX_ROWS, 1, BoundedXlsx.MAX_ROWS),
                FileTypePolicy.from(
                        configurations.value(ConfigurationKeys.FILE_FILES),
                        configurations.value(ConfigurationKeys.FILE_MEDIA),
                        configurations.value(ConfigurationKeys.FILE_IMAGES)));
    }

    private String text(String key) {
        String value = configurations.value(key);
        return value == null || value.isBlank() ? null : value.strip();
    }

    private boolean flag(String key) {
        String value = configurations.value(key);
        return "1".equals(value == null ? null : value.strip());
    }

    private int boundedInt(String key, int fallback, int min, int max) {
        String value = configurations.value(key);
        if (value == null || value.isBlank()) return fallback;
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(value.strip())));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private long megabytes(String key) {
        String value = configurations.value(key);
        if (value == null || value.isBlank()) return Long.MAX_VALUE;
        try {
            long megabytes = Long.parseLong(value.strip());
            if (megabytes <= 0 || megabytes > Long.MAX_VALUE / MB) return Long.MAX_VALUE;
            return megabytes * MB;
        } catch (NumberFormatException ignored) {
            return Long.MAX_VALUE;
        }
    }

    public record RuntimeSettings(String copyrightText, String contactPhone, boolean maintenanceMode,
                                  int passwordExpireDays, long fileMaxBytes, int exportMaxRows,
                                  FileTypePolicy fileTypes) {}
}
