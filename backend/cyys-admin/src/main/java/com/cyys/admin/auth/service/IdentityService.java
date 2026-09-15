package com.cyys.admin.auth.service;

import com.cyys.admin.auth.dto.LoginVO;
import com.cyys.admin.scope.mapper.SysScopeMapper;
import com.cyys.admin.scope.model.SysScope;
import com.cyys.admin.user.mapper.SysUserMapper;
import com.cyys.admin.user.mapper.SysUserRoleMapper;
import com.cyys.admin.user.mapper.SysUserScopeMapper;
import com.cyys.admin.user.model.SysUser;
import com.cyys.common.satoken.DataIsolationProperties;
import com.cyys.common.satoken.IdentityResolver;
import com.cyys.common.satoken.RequestIdentity;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class IdentityService implements IdentityResolver {
    public static final String GLOBAL = "GLOBAL";
    public static final String PLATFORM_SCOPE_ACCESS = "platform:scope:access";

    private final SysUserMapper userMapper;
    private final SysUserScopeMapper membershipMapper;
    private final SysScopeMapper scopeMapper;
    private final SysUserRoleMapper roleMapper;
    private final DataIsolationProperties isolation;

    @Override
    public RequestIdentity resolve(String loginId, String requestedScopeId, String stamp) {
        SysUser user = userMapper.selectById(loginId);
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
        SysUser user = userMapper.selectById(loginId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new ApiException(401, "账号已失效");
        }
        return userInfo(snapshot(user, scopeId, false));
    }

    private Snapshot snapshot(SysUser user, String requestedScopeId, boolean chooseDefault) {
        List<String> platformPermissions = roleMapper.selectPermissionCodes(user.getId(), List.of(GLOBAL));
        boolean canAccessUnits = platformPermissions.contains(PLATFORM_SCOPE_ACCESS);
        List<LoginVO.ScopeInfo> memberships = membershipMapper.selectAvailableScopes(user.getId());
        if (memberships.stream().filter(LoginVO.ScopeInfo::defaultFlag).count() > 1) {
            throw new IllegalStateException("账号存在多个默认单位，需要修复成员数据");
        }

        List<LoginVO.ScopeInfo> availableScopes;
        String selectedScopeId;
        if (isolation.mode() == DataIsolationProperties.Mode.SINGLE) {
            SysScope fixedScope = scopeMapper.selectEnabledById(isolation.singleScopeId());
            if (fixedScope == null) {
                throw new ApiException(403, "配置的固定单位当前不可用");
            }
            if (requestedScopeId != null && !requestedScopeId.equals(fixedScope.getId())) {
                throw new ApiException(403, "单单位模式不能选择其他单位");
            }
            LoginVO.ScopeInfo memberScope = memberships.stream()
                    .filter(scope -> scope.id().equals(fixedScope.getId())).findFirst().orElse(null);
            if (memberScope == null && !canAccessUnits) {
                throw new ApiException(403, "没有固定单位的有效成员关系");
            }
            availableScopes = List.of(memberScope != null ? memberScope : platformScope(fixedScope));
            selectedScopeId = fixedScope.getId();
        } else {
            availableScopes = canAccessUnits ? platformScopes(memberships) : memberships;
            if (availableScopes.isEmpty() && !canAccessUnits) {
                throw new ApiException(403, "没有可访问的有效单位");
            }
            if (requestedScopeId != null && (requestedScopeId.isBlank()
                    || availableScopes.stream().noneMatch(scope -> scope.id().equals(requestedScopeId)))) {
                throw new ApiException(403, "所选单位不存在或未获授权");
            }
            selectedScopeId = requestedScopeId;
            // 登录可选择已有有效成员关系中的默认单位。后续请求不从会话补单位。
            if (chooseDefault && selectedScopeId == null && !memberships.isEmpty()) {
                selectedScopeId = memberships.stream().filter(LoginVO.ScopeInfo::defaultFlag)
                        .findFirst().orElse(memberships.getFirst()).id();
            }
        }

        List<String> effectiveScopes = new ArrayList<>(List.of(GLOBAL));
        if (selectedScopeId != null) {
            effectiveScopes.add(selectedScopeId);
        }
        List<String> unitPermissions = selectedScopeId == null ? List.of()
                : roleMapper.selectPermissionCodes(user.getId(), List.of(selectedScopeId));
        List<String> permissions = java.util.stream.Stream.concat(platformPermissions.stream(), unitPermissions.stream())
                .distinct().sorted().toList();
        RequestIdentity identity = new RequestIdentity(user.getId(), selectedScopeId,
                roleMapper.selectRoleCodes(user.getId(), effectiveScopes),
                permissions, platformPermissions);
        return new Snapshot(user, identity, List.copyOf(availableScopes), unitPermissions);
    }

    private List<LoginVO.ScopeInfo> platformScopes(List<LoginVO.ScopeInfo> memberships) {
        return scopeMapper.selectEnabledScopes().stream().map(scope -> memberships.stream()
                .filter(member -> member.id().equals(scope.getId())).findFirst()
                .orElseGet(() -> platformScope(scope))).toList();
    }

    private LoginVO.ScopeInfo platformScope(SysScope scope) {
        return new LoginVO.ScopeInfo(scope.getId(), scope.getCode(), scope.getName(), false, null);
    }

    public LoginVO.UserInfo userInfo(Snapshot snapshot) {
        SysUser user = snapshot.user();
        RequestIdentity identity = snapshot.identity();
        LoginVO.ScopeInfo defaultScope = snapshot.scopes().stream()
                .filter(LoginVO.ScopeInfo::defaultFlag).findFirst().orElse(null);
        if (isolation.mode() == DataIsolationProperties.Mode.SINGLE) {
            defaultScope = snapshot.scopes().getFirst();
        }
        String displayName = user.getNickname();
        if (displayName == null || displayName.isBlank()) {
            displayName = user.getRealName();
        }
        if (displayName == null || displayName.isBlank()) {
            displayName = user.getLoginName();
        }
        return new LoginVO.UserInfo(user.getId(), user.getLoginName(), user.getRealName(), displayName,
                user.getNickname(), user.getAvatar(), user.getPhone(), user.getEmail(), user.getStatus(),
                user.getLastLoginTime(), isolation.mode().name().toLowerCase(java.util.Locale.ROOT),
                snapshot.scopes(), defaultScope, identity.scopeId(), identity.roles(),
                identity.permissions(), identity.platformPermissions(), snapshot.unitPermissions());
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

    public record Snapshot(SysUser user, RequestIdentity identity, List<LoginVO.ScopeInfo> scopes, List<String> unitPermissions) {
    }
}
