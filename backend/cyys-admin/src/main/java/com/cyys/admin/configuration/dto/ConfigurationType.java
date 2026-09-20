package com.cyys.admin.configuration.dto;

public enum ConfigurationType {
    SYSTEM(1), THIRD_PARTY(2);

    private final int code;
    ConfigurationType(int code) { this.code = code; }
    public int code() { return code; }

    public static ConfigurationType fromCode(int code) {
        return switch (code) {
            case 1 -> SYSTEM;
            case 2 -> THIRD_PARTY;
            default -> throw new IllegalStateException("未知配置类别：" + code);
        };
    }
}
