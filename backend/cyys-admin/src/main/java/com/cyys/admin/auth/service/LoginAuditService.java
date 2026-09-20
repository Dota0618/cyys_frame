package com.cyys.admin.auth.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.auth.dto.LoginAuditQuery;
import com.cyys.admin.auth.dto.LoginAuditView;
import com.cyys.admin.auth.mapper.DataAuthorizationMapper;
import com.cyys.admin.auth.mapper.LoginAuditMapper;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginAuditService {
    private final LoginAuditMapper mapper;
    private final DataAuthorizationMapper authorization;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String loginName, String userId, String scopeId, String clientIp,
                       String outcome, String reason) {
        if (mapper.insert(IdWorker.getIdStr(), loginName, userId, scopeId, clientIp, outcome, reason) != 1) {
            throw new IllegalStateException("登录记录写入失败");
        }
    }

    @Transactional(readOnly = true)
    public IPage<LoginAuditView> page(LoginAuditQuery query) {
        var identity = ScopeContext.get();
        String scope = identity.requireScopeId();
        if (!authorization.hasPermission(identity.loginId(), scope, "login-audit:read")) {
            throw new ApiException(403, "没有当前单位的登录记录查看权限");
        }
        return mapper.page(new Page<>(query.page(), query.size()), scope,
                blankToNull(query.loginName()), blankToNull(query.outcome()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
