package com.cyys.admin.audit.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.audit.dto.AuditQuery;
import com.cyys.admin.audit.dto.AuditView;
import com.cyys.admin.audit.mapper.DataAuditMapper;
import com.cyys.admin.auth.mapper.DataAuthorizationMapper;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditQueryService {
    private final DataAuditMapper mapper;
    private final DataAuthorizationMapper authorization;

    @Transactional(readOnly = true)
    public IPage<AuditView> page(AuditQuery query) {
        var identity = ScopeContext.get();
        String scope = identity.requireScopeId();
        if (!authorization.hasPermission(identity.loginId(), "audit:read")) {
            throw new ApiException(403, "没有当前单位的审计查看权限");
        }
        return mapper.page(new Page<>(query.page(), query.size()), scope,
                blankToNull(query.resource()), blankToNull(query.operation()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
