package com.cyys.admin.audit.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cyys.admin.audit.mapper.DataAuditMapper;
import com.cyys.common.satoken.ScopeContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DataAuditService {
    private final DataAuditMapper mapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void success(String resource, String record, String operation) {
        var identity = ScopeContext.get();
        if (mapper.insertSuccess(IdWorker.getIdStr(), identity.loginId(), identity.requireScopeId(), resource, record, operation) != 1) {
            throw new IllegalStateException("业务审计写入失败");
        }
    }
}
