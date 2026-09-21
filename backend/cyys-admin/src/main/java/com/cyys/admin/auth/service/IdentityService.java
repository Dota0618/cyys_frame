package com.cyys.admin.auth.service;

import com.cyys.admin.auth.dto.LoginVO;
import com.cyys.admin.auth.dto.ScopeInfo;
import com.cyys.admin.user.mapper.SysUserMapper;
import com.cyys.admin.user.mapper.SysUserRoleMapper;
import com.cyys.admin.user.mapper.SysUserScopeMapper;
import com.cyys.admin.user.model.SysUser;
import com.cyys.common.satoken.IdentityResolver;
import com.cyys.common.satoken.RequestIdentity;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class IdentityService implements IdentityResolver {
    public static final String GLOBAL = "GLOBAL";

    private final SysUserMapper userMapper;
    private final SysUserScopeMapper membershipMapper;
    private final SysUserRoleMapper roleMapper;

    @Override
    public RequestIdentity resolve(String loginId, String requestedScopeId, String stamp) {
        SysUser user = userMapper.selectAccountById(loginId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())
                || stamp == null || !MessageDigest.isEqual(
                        credentialStamp(user).getBytes(StandardCharsets.US_ASCII),
                        stamp.getBytes(StandardCharsets.US_ASCII))) {
            throw new ApiException(401, "账号状态或密码已变更，请重新登录");
        }
        return snapshot(user, requestedScopeId, false).identity();
    }

    public Snapshot forLogin(SysUser user, String requestedScopeId) {
        return snapshot(user, requestedScopeId, true);
    }

    public LoginVO.UserInfo currentUser(String loginId, String scopeId) {
        SysUser user = userMapper.selectAccountById(loginId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new ApiException(401, "账号已失效");
        }
        return userInfo(snapshot(user, scopeId, false));
    }

    private Snapshot snapshot(SysUser user, String requestedScopeId, boolean chooseDefault) {
        var profile = membershipMapper.profile(user.getId());
        if (profile == null) throw new ApiException(403, "用户资料不存在或已停用");
        List<ScopeInfo> availableScopes = membershipMapper.selectAvailableScopes(user.getId());
        if (requestedScopeId != null && (requestedScopeId.isBlank()
                || availableScopes.stream().noneMatch(scope -> scope.id().equals(requestedScopeId)))) {
            throw new ApiException(403, "所选机构不在当前用户的数据范围内");
        }
        String selectedScopeId = requestedScopeId;
        if (chooseDefault && selectedScopeId == null && !availableScopes.isEmpty()) {
            selectedScopeId = availableScopes.stream().filter(ScopeInfo::defaultFlag)
                    .findFirst().orElse(availableScopes.getFirst()).id();
        }
        List<String> permissions = roleMapper.selectPermissionCodes(user.getId());
        RequestIdentity identity = new RequestIdentity(user.getId(), selectedScopeId,
                roleMapper.selectRoleCodes(user.getId()), permissions);
        return new Snapshot(user, identity, List.copyOf(availableScopes), profile);
    }
    public LoginVO.UserInfo userInfo(Snapshot snapshot) {
        SysUser user = snapshot.user();
        RequestIdentity identity = snapshot.identity();
        ScopeInfo defaultScope = snapshot.scopes().stream()
                .filter(ScopeInfo::defaultFlag).findFirst().orElse(null);
        String displayName = snapshot.profile().getDisplayName();
        if (displayName == null || displayName.isBlank()) displayName = user.getNickname();
        if (displayName == null || displayName.isBlank()) {
            displayName = user.getRealName();
        }
        if (displayName == null || displayName.isBlank()) {
            displayName = user.getLoginName();
        }
        return new LoginVO.UserInfo(user.getId(), user.getLoginName(), user.getRealName(), displayName,
                user.getNickname(), user.getAvatar(), user.getPhone(), user.getEmail(), user.getStatus(),
                user.getLastLoginTime(),
                snapshot.scopes(), defaultScope, identity.scopeId(), identity.roles(),
                identity.permissions(),
                roleMapper.selectNavigation(user.getId()), snapshot.profile().getScopeId(), snapshot.profile().getOrgId(), snapshot.profile().getDataRange());
    }

    static String credentialStamp(SysUser user) {
        try {
            byte[] source = (Objects.requireNonNull(user.getPasswordHash()) + "\n"
                    + Objects.toString(user.getPwdLastChanged(), "")).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("JDK 缺少 SHA-256", error);
        }
    }

    public record Snapshot(SysUser user, RequestIdentity identity, List<ScopeInfo> scopes, com.cyys.admin.user.model.SysUserScope profile) {
    }
}
