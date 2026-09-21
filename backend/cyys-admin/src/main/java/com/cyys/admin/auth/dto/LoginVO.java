package com.cyys.admin.auth.dto;

import java.time.LocalDateTime;
import java.util.List;
import com.cyys.admin.menu.dto.NavigationMenu;

public record LoginVO(String tokenName, String tokenType, String token, long expiresIn, UserInfo userInfo) {
    public record UserInfo(String id, String loginName, String realName, String displayName,
                           String nickname, String avatar, String phone, String email, Integer status,
                           LocalDateTime lastLoginTime, List<ScopeInfo> scopes,
                           ScopeInfo defaultScope, String currentScopeId, List<String> roles,
                           List<String> permissions,
                           List<NavigationMenu> navigation, String homeScopeId, String orgId, int dataRange) {
        public UserInfo {
            scopes = List.copyOf(scopes);
            roles = List.copyOf(roles);
            permissions = List.copyOf(permissions);
            navigation = List.copyOf(navigation);
        }
    }

    /** defaultFlag 表示用户所属机构；候选来自数据范围，parentId 用于机构树。 */
    public record ScopeInfo(String id, String code, String name, boolean defaultFlag, String parentId) {
    }
}
