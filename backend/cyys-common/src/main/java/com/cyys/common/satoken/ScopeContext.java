package com.cyys.common.satoken;

import java.util.Objects;

public final class ScopeContext {
    private static final ThreadLocal<RequestIdentity> CURRENT = new ThreadLocal<>();

    private ScopeContext() {
    }

    public static RequestIdentity get() {
        RequestIdentity identity = CURRENT.get();
        if (identity == null) {
            throw new IllegalStateException("可信请求上下文尚未建立");
        }
        return identity;
    }

    public static void set(RequestIdentity identity) {
        if (CURRENT.get() != null) {
            throw new IllegalStateException("同一请求或事务内不得替换身份/单位上下文");
        }
        CURRENT.set(Objects.requireNonNull(identity));
    }

    public static void clear() {
        CURRENT.remove();
    }

    /** 仅供清理与失败审计观察；业务授权仍使用 get()/requireScopeId()。 */
    public static java.util.Optional<RequestIdentity> current() {
        return java.util.Optional.ofNullable(CURRENT.get());
    }
}
