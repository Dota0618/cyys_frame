package com.cyys.common.satoken;

import com.cyys.common.web.ApiException;
import java.util.List;
import java.util.Objects;

/** 一次请求验证后的不可变身份。scopeId 为空仅表示尚未选择机构；是否允许无筛选查询由资源策略决定。 */
public record RequestIdentity(String loginId, String scopeId, List<String> roles,
                              List<String> permissions) {
    public RequestIdentity {
        Objects.requireNonNull(loginId, "loginId");
        roles = List.copyOf(roles);
        permissions = List.copyOf(permissions);
    }

    public String requireScopeId() {
        if (scopeId == null) {
            throw new ApiException(403, "请在右上角选择机构");
        }
        return scopeId;
    }
}
