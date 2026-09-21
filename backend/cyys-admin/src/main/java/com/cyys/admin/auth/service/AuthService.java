package com.cyys.admin.auth.service;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.cyys.admin.auth.dto.LoginCmd;
import com.cyys.admin.auth.dto.LoginVO;
import com.cyys.admin.user.mapper.SysUserMapper;
import com.cyys.admin.user.model.SysUser;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.satoken.ScopeHandlerInterceptor;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final SysUserMapper userMapper;
    private final IdentityService identityService;
    private final LoginProperties loginProperties;
    private final LoginAuditService loginAuditService;
    private final TransactionTemplate transactions;
    private final PasswordService passwords;

    public void changePassword(com.cyys.admin.auth.dto.ChangePasswordCommand command) {
        passwords.change(command);
    }

    public LoginVO login(LoginCmd command, String clientIp) {
        if (command.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(400, "密码的 UTF-8 长度不能超过 72 字节");
        }
        // 密码失败先提交计数，再抛请求错误；成功身份在事务提交后才创建会话。
        LoginAttempt attempt = Objects.requireNonNull(transactions.execute(status -> authenticate(command, clientIp)));
        if (attempt.failure() != null) {
            throw attempt.failure();
        }

        IdentityService.Snapshot snapshot = attempt.snapshot();
        String createdToken = null;
        try {
            StpUtil.login(snapshot.user().getId());
            createdToken = StpUtil.getTokenValue();
            StpUtil.getTokenSession().set(ScopeHandlerInterceptor.CREDENTIAL_STAMP,
                    IdentityService.credentialStamp(snapshot.user()));
            return new LoginVO(StpUtil.getTokenName(), "Bearer", createdToken,
                    StpUtil.getTokenTimeout(), identityService.userInfo(snapshot));
        } catch (RuntimeException error) {
            if (createdToken != null) {
                try {
                    StpUtil.logoutByTokenValue(createdToken);
                } catch (RuntimeException cleanupError) {
                    error.addSuppressed(cleanupError);
                }
            }
            throw error;
        }
    }

    private LoginAttempt authenticate(LoginCmd command, String clientIp) {
        String loginName = command.loginName().trim();
        SysUser user = userMapper.selectForLogin(loginName);
        if (user == null) {
            loginAuditService.record(loginName, null, null, clientIp, "FAILURE", "INVALID_CREDENTIALS");
            return rejected(401, "账号或密码错误");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            recordRejected(command, user, clientIp, "DISABLED", "ACCOUNT_DISABLED");
            return rejected(403, "账号已被禁用");
        }

        LocalDateTime now = LocalDateTime.now();
        if (user.getPwdLockTime() != null && user.getPwdLockTime().isAfter(now)) {
            recordRejected(command, user, clientIp, "LOCKED", "PASSWORD_LOCKED");
            return rejected(423, "密码错误次数过多，请在锁定结束后重试");
        }
        boolean passwordMatches;
        try {
            passwordMatches = BCrypt.checkpw(command.password(), user.getPasswordHash());
        } catch (IllegalArgumentException error) {
            // 旧散列不自动降级验证；迁移时先重置为 BCrypt，再允许新系统登录。
            log.warn("账号密码格式不受支持，需要迁移重置，userId={}", user.getId());
            recordRejected(command, user, clientIp, "FAILURE", "INVALID_CREDENTIALS");
            return rejected(401, "账号或密码错误");
        }
        if (!passwordMatches) {
            int nextCount = user.getPwdLockTime() != null ? 1 : user.getPwdErrorCount() + 1;
            boolean locked = nextCount >= loginProperties.getLoginMaxErrorCount();
            LocalDateTime lockedUntil = locked ? now.plusMinutes(loginProperties.getLockMinutes()) : null;
            requireUpdated(userMapper.recordPasswordFailure(user.getId(), nextCount, lockedUntil));
            recordRejected(command, user, clientIp,
                    locked ? "LOCKED" : "FAILURE", locked ? "PASSWORD_LOCKED" : "INVALID_CREDENTIALS");
            return rejected(locked ? 423 : 401, locked ? "密码错误次数过多，账号已暂时锁定" : "账号或密码错误");
        }

        IdentityService.Snapshot snapshot;
        try {
            snapshot = identityService.forLogin(user, command.scopeId());
        } catch (ApiException denied) {
            loginAuditService.record(loginName, user.getId(), null, clientIp, "FAILURE", "SCOPE_DENIED");
            return new LoginAttempt(null, denied);
        }
        requireUpdated(userMapper.recordLoginSuccess(user.getId(), now, clientIp));
        loginAuditService.record(loginName, user.getId(), snapshot.identity().scopeId(), clientIp, "SUCCESS", null);
        user.setLastLoginTime(now);
        return new LoginAttempt(snapshot, null);
    }

    public void logout() {
        StpUtil.logout();
    }

    private void recordRejected(LoginCmd command, SysUser user, String clientIp, String outcome, String reason) {
        String verifiedScope = null;
        try {
            verifiedScope = identityService.forLogin(user, command.scopeId()).identity().scopeId();
        } catch (ApiException denied) {
            // 无法确认归属时保留无单位的安全记录，不信任登录请求中的单位。
        }
        loginAuditService.record(user.getLoginName(), user.getId(), verifiedScope, clientIp, outcome, reason);
    }

    public LoginVO.UserInfo me() {
        var identity = ScopeContext.get();
        return identityService.currentUser(identity.loginId(), identity.scopeId());
    }

    private LoginAttempt rejected(int status, String message) {
        return new LoginAttempt(null, new ApiException(status, message));
    }

    private void requireUpdated(int rows) {
        if (rows != 1) {
            throw new IllegalStateException("登录状态更新未影响唯一账号");
        }
    }

    private record LoginAttempt(IdentityService.Snapshot snapshot, ApiException failure) {
    }
}
