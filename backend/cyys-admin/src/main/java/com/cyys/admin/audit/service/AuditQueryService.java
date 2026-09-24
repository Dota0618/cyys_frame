package com.cyys.admin.audit.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.audit.dto.AuditQuery;
import com.cyys.admin.audit.dto.AuditOptions;
import com.cyys.admin.audit.dto.AuditView;
import com.cyys.admin.audit.mapper.DataAuditMapper;
import com.cyys.admin.auth.mapper.DataAuthorizationMapper;
import com.cyys.admin.dictionary.service.DictionaryLookupService;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditQueryService {
    private static final String OPERATIONS = "DATA_AUDIT_OPERATION";
    private static final String OUTCOMES = "DATA_AUDIT_OUTCOME";
    private final DataAuditMapper mapper;
    private final DataAuthorizationMapper authorization;
    private final DictionaryLookupService dictionaries;

    @Transactional(readOnly = true)
    public AuditOptions options() {
        authorize();
        return new AuditOptions(dictionaries.options(OPERATIONS), dictionaries.options(OUTCOMES));
    }

    @Transactional(readOnly = true)
    public IPage<AuditView> page(AuditQuery query) {
        String scope = authorize();
        return mapper.page(new Page<>(query.page(), query.size()), scope,
                blankToNull(query.resource()), blankToNull(query.operation()), blankToNull(query.outcome()));
    }

    private String authorize() {
        var identity = ScopeContext.get();
        String scope = identity.requireScopeId();
        if (!authorization.hasPermission(identity.loginId(), "audit:read")) {
            throw new ApiException(403, "没有当前单位的审计查看权限");
        }
        return scope;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
