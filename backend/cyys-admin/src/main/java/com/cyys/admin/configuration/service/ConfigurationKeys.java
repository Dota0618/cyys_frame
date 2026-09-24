package com.cyys.admin.configuration.service;

import java.util.List;

public final class ConfigurationKeys {
    public static final String SYSTEM_NAME = "SYSTEM_NAME";
    public static final String SYSTEM_PROVINCE_CODE = "SYSTEM_PROVINCE_CODE";
    public static final String PWD_STRENGTH = "PWD_STRENGTH";
    public static final String PWD_LOCK_NUM = "PWD_LOCK_NUM";
    public static final String PWD_LOCK_HOUR = "PWD_LOCK_HOUR";
    public static final String COPYRIGHT_TEXT = "COPYRIGHT_TEXT";
    public static final String MAINTENANCE_MODE = "MAINTENANCE_MODE";
    public static final String PASSWORD_EXPIRE_DAYS = "PASSWORD_EXPIRE_DAYS";
    public static final String FILE_MAX_SIZE_MB = "FILE_MAX_SIZE_MB";
    public static final String FILE_FILES = "FILE_FILES";
    public static final String FILE_MEDIA = "FILE_MEDIA";
    public static final String FILE_IMAGES = "FILE_IMAGES";
    public static final String EXPORT_MAX_ROWS = "EXPORT_MAX_ROWS";
    public static final String AUDIT_RETENTION_DAYS = "AUDIT_RETENTION_DAYS";
    public static final String CONTACT_PHONE = "CONTACT_PHONE";

    public static final List<String> PUBLIC_KEYS = List.of(
            SYSTEM_NAME, SYSTEM_PROVINCE_CODE, PWD_STRENGTH, PWD_LOCK_NUM, PWD_LOCK_HOUR,
            COPYRIGHT_TEXT, MAINTENANCE_MODE, CONTACT_PHONE, FILE_FILES, FILE_MEDIA, FILE_IMAGES);

    private ConfigurationKeys() {}
}
