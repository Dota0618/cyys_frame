package com.cyys.common.satoken;

import com.cyys.common.web.ApiException;
import java.util.List;
import java.util.Objects;

/** 一次请求验证后的不可变身份。scopeId 为空仅表示尚未选择单位，不代表全部单位。 */
public record RequestIdentity(String loginId, String scopeId, List<String> roles,
                              List<String> permissions, List<String> platformPermissions) {
    public RequestIdentity {
        Objects.requireNonNull(loginId, "loginId");
        roles = List.copyOf(roles);
        permissions = List.copyOf(permissions);
        platformPermissions = List.copyOf(platformPermissions);
    }

    public String requireScopeId() {
        if (scopeId == null) {
            throw new ApiException(403, "请先选择有权访问的单位");
        }
        return scopeId;
    }
}
