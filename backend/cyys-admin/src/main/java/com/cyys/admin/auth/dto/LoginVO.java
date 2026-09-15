package com.cyys.admin.auth.dto;

import java.time.LocalDateTime;
import java.util.List;

public record LoginVO(String tokenName, String tokenType, String token, long expiresIn, UserInfo userInfo) {
    public record UserInfo(String id, String loginName, String realName, String displayName,
                           String nickname, String avatar, String phone, String email, Integer status,
                           LocalDateTime lastLoginTime, String mode, List<ScopeInfo> scopes,
                           ScopeInfo defaultScope, String currentScopeId, List<String> roles,
                           List<String> permissions, List<String> platformPermissions, List<String> unitPermissions) {
        public UserInfo {
            scopes = List.copyOf(scopes);
            roles = List.copyOf(roles);
            permissions = List.copyOf(permissions);
            platformPermissions = List.copyOf(platformPermissions);
            unitPermissions = List.copyOf(unitPermissions);
        }
    }

    /** memberRole 只来自实际成员关系；平台直接访问的单位不伪造成 OWNER。 */
    public record ScopeInfo(String id, String code, String name, boolean defaultFlag, String memberRole) {
    }
}
