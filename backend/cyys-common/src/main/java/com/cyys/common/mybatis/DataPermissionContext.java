package com.cyys.common.mybatis;

import java.util.Objects;

/** 当前接口已经声明的功能权限；数据范围仍从数据库中的用户角色动态计算。 */
public final class DataPermissionContext {
    private static final ThreadLocal<Current> CURRENT = new ThreadLocal<>();

    private DataPermissionContext() {}

    public static void set(String permission, DataPolicyRegistry.View view) {
        if (CURRENT.get() != null) throw new IllegalStateException("同一请求不得替换功能权限上下文");
        CURRENT.set(new Current(permission, view));
    }

    public static Current get() {
        Current current = CURRENT.get();
        if (current == null) throw DataPolicyRegistry.denied("受保护数据访问缺少功能权限上下文");
        return current;
    }

    public static void clear() {
        CURRENT.remove();
    }

    public record Current(String permission, DataPolicyRegistry.View view) {
        public Current {
            Objects.requireNonNull(view, "view");
            if (permission == null || !permission.matches("[a-z][a-z0-9:_-]+")) {
                throw DataPolicyRegistry.denied("功能权限标识无效");
            }
        }

        public DataPolicyRegistry.Access access() {
            return permission.endsWith(":read")
                    ? DataPolicyRegistry.Access.READ : DataPolicyRegistry.Access.WRITE;
        }
    }
}
