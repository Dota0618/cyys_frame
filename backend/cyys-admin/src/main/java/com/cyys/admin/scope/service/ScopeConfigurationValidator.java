package com.cyys.admin.scope.service;

import com.cyys.admin.scope.mapper.SysScopeMapper;
import com.cyys.common.satoken.DataIsolationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScopeConfigurationValidator implements ApplicationRunner {
    private final DataIsolationProperties isolation;
    private final SysScopeMapper scopeMapper;

    @Override
    public void run(ApplicationArguments args) {
        if (isolation.mode() == DataIsolationProperties.Mode.SINGLE
                && scopeMapper.selectEnabledById(isolation.singleScopeId()) == null) {
            throw new IllegalStateException("single-scope-id 必须是已存在且启用的 sys_scope.id");
        }
    }
}
