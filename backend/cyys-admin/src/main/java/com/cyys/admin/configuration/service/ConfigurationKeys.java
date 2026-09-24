package com.cyys.admin.configuration.service;

import java.util.List;

public final class ConfigurationKeys {
    public static final String SYSTEM_NAME = "SYSTEM_NAME";
    public static final String SYSTEM_PROVINCE_CODE = "SYSTEM_PROVINCE_CODE";
    public static final String PWD_STRENGTH = "PWD_STRENGTH";
    public static final String PWD_LOCK_NUM = "PWD_LOCK_NUM";
    public static final String PWD_LOCK_HOUR = "PWD_LOCK_HOUR";

    public static final List<String> PUBLIC_KEYS = List.of(
            SYSTEM_NAME, SYSTEM_PROVINCE_CODE, PWD_STRENGTH, PWD_LOCK_NUM, PWD_LOCK_HOUR);

    private ConfigurationKeys() {}
}
