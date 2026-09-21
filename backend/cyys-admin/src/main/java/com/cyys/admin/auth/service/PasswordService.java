package com.cyys.admin.auth.service;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.auth.dto.ChangePasswordCommand;
import com.cyys.admin.auth.dto.ResetPasswordCommand;
import com.cyys.admin.user.mapper.SysUserMapper;
import com.cyys.admin.user.model.SysUser;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.satoken.ScopeHandlerInterceptor;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PasswordService {
    private final SysUserMapper users;
    private final PlatformSecurityService platform;
    private final LoginProperties settings;
    private final DataAuditService audit;
    private final TransactionTemplate transactions;

    @Transactional(propagation=Propagation.NEVER)
    public void change(ChangePasswordCommand command) {
        PasswordPolicy.validateNew(command.newPassword());
        PasswordPolicy.validateBytes(command.currentPassword());
        String actor=ScopeContext.get().loginId();
        String stamp=currentStamp();
        // 错误计数独立提交，再向客户端返回失败，避免事务回滚抹掉限流计数。
        ApiException rejected=transactions.execute(status -> {
            var account=require(actor);
            ApiException failure=verifyOperator(account,stamp,command.currentPassword());
            if(failure!=null) return failure;
            if(command.newPassword().equals(command.currentPassword())) return new ApiException(400,"新密码不能与原密码相同");
            replace(account,command.newPassword(),actor,"CHANGE_PASSWORD");
            return null;
        });
        if(rejected!=null) throw rejected;
    }

    @Transactional(propagation=Propagation.NEVER)
    public void reset(String target,ResetPasswordCommand command) {
        platform.authorize("platform:account:reset-password");
        platform.requireAllDataRange();
        PasswordPolicy.validateNew(command.newPassword());
        String actor=ScopeContext.get().loginId();
        String stamp=currentStamp();
        ApiException rejected=transactions.execute(status -> {
            platform.lockAdministratorGuard();
            // 两名管理员互相重置时仍按同一 ID 顺序加锁，避免相反锁顺序。
            var locked=new HashMap<String,SysUser>();
            for(String id:List.of(actor,target).stream().distinct().sorted().toList()) locked.put(id,require(id));
            platform.authorize("platform:account:reset-password");
            platform.requireAllDataRange();
            ApiException failure=verifySession(locked.get(actor),stamp);
            if(failure!=null) return failure;
            replace(locked.get(target),command.newPassword(),actor,"RESET_PASSWORD");
            return null;
        });
        if(rejected!=null) throw rejected;
    }

    private SysUser require(String id) {
        var account=users.lockAccount(id);
        if(account==null) throw new ApiException(404,"账号不存在");
        return account;
    }

    private ApiException verifySession(SysUser account,String stamp) {
        if(account.getStatus()!=1 || stamp==null || !MessageDigest.isEqual(
                IdentityService.credentialStamp(account).getBytes(StandardCharsets.US_ASCII),stamp.getBytes(StandardCharsets.US_ASCII))) {
            return new ApiException(401,"账号状态或密码已变更，请重新登录");
        }
        return null;
    }

    private ApiException verifyOperator(SysUser account,String stamp,String password) {
        var sessionFailure=verifySession(account,stamp);
        if(sessionFailure!=null) return sessionFailure;
        var now=LocalDateTime.now();
        if(account.getPwdLockTime()!=null && account.getPwdLockTime().isAfter(now)) return new ApiException(423,"密码校验已暂时锁定，请稍后重试");
        boolean matches;
        try { matches=BCrypt.checkpw(password,account.getPasswordHash()); }
        catch(IllegalArgumentException invalidHash) { matches=false; }
        if(matches) return null;
        int failures=account.getPwdLockTime()!=null?1:account.getPwdErrorCount()+1;
        boolean lock=failures>=settings.getLoginMaxErrorCount();
        requireOne(users.recordPasswordFailure(account.getId(),failures,lock?now.plusMinutes(settings.getLockMinutes()):null));
        return new ApiException(lock?423:400,lock?"密码错误次数过多，已暂时锁定":"当前密码不正确");
    }

    private void replace(SysUser target,String password,String actor,String operation) {
        requireOne(users.replacePassword(target.getId(),PasswordPolicy.hash(password),actor));
        if ("CHANGE_PASSWORD".equals(operation)) audit.ownPasswordChanged();
        else audit.platformSuccess("account",target.getId(),operation);
        // 每次使用新盐；现有 IdentityService 逐请求比较散列/改密时间指纹，旧会话立即失效。
        // 不在提交后按账号批量登出，以免误踢提交后刚建立的新密码会话。
    }

    private String currentStamp() { return (String)StpUtil.getTokenSession().get(ScopeHandlerInterceptor.CREDENTIAL_STAMP); }
    private static void requireOne(int count) { if(count!=1) throw new ApiException(409,"账号状态已变化，请重试"); }
}
