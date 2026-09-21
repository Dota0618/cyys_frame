package com.cyys.common.mybatis;

import java.util.Objects;

/** 数据视图独立于功能权限；账号范围由统一数据权限插件计算。 */
public final class DataPermissionContext {
    private static final ThreadLocal<Current> CURRENT = new ThreadLocal<>();

    private DataPermissionContext() {}

    public static void set(DataPolicyRegistry.View view) {
        if (CURRENT.get() != null) throw new IllegalStateException("同一请求不得替换数据范围上下文");
        CURRENT.set(new Current(Objects.requireNonNull(view, "view")));
    }

    public static Current get() {
        Current current = CURRENT.get();
        if (current == null) throw DataPolicyRegistry.denied("受保护数据访问缺少数据范围上下文");
        return current;
    }

    public static void clear() {
        CURRENT.remove();
    }

    public record Current(DataPolicyRegistry.View view) {}
}
