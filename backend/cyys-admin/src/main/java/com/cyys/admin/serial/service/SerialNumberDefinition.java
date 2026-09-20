package com.cyys.admin.serial.service;

import java.time.ZoneId;

/** 部署端规则，不接受普通业务请求提交模板、周期或单位。 */
public record SerialNumberDefinition(String prefix, SerialPeriod period, int width, String zone, String permission) {
    public SerialNumberDefinition {
        // 前缀只在末尾使用分隔符，避免另一业务前缀吞入日期段造成完整号码碰撞。
        if (prefix == null || !prefix.matches("[A-Z][A-Z0-9_]{0,30}-") || period == null || width < 1 || width > 18
                || zone == null || permission == null || permission.length() > 100
                || !permission.matches("[a-z][a-z0-9:_-]+") || permission.endsWith(":read")) {
            throw new IllegalArgumentException("流水号必须配置固定前缀、周期、1–18 位宽度、时区及业务写操作权限");
        }
        zone = ZoneId.of(zone).getId();
    }
}
