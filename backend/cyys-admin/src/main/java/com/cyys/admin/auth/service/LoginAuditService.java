package com.cyys.admin.auth.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.auth.dto.LoginAuditQuery;
import com.cyys.admin.auth.dto.LoginAuditOptions;
import com.cyys.admin.auth.dto.LoginAuditView;
import com.cyys.admin.dictionary.service.DictionaryLookupService;
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
    private static final String OUTCOMES = "LOGIN_AUDIT_OUTCOME";
    private final LoginAuditMapper mapper;
    private final DataAuthorizationMapper authorization;
    private final DictionaryLookupService dictionaries;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String loginName, String realName, String userId, String scopeId, String clientIp,
                       String outcome, String reason) {
        if (mapper.insert(IdWorker.getIdStr(), loginName, realName, userId, scopeId, clientIp, outcome, reason) != 1) {
            throw new IllegalStateException("登录记录写入失败");
        }
    }

    @Transactional(readOnly = true)
    public LoginAuditOptions options() {
        return new LoginAuditOptions(dictionaries.options(OUTCOMES));
    }

    @Transactional(readOnly = true)
    public IPage<LoginAuditView> page(LoginAuditQuery query) {
        var identity = ScopeContext.get();
        String scope = identity.requireScopeId();
        if (!authorization.hasPermission(identity.loginId(), "login-audit:read")) {
            throw new ApiException(403, "没有当前单位的登录记录查看权限");
        }
        return mapper.page(new Page<>(query.page(), query.size()), scope,
                blankToNull(query.loginName()), blankToNull(query.outcome()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
