package com.cyys.application;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.auth.dto.LoginAuditQuery;
import com.cyys.admin.auth.dto.LoginAuditView;
import com.cyys.admin.auth.mapper.DataAuthorizationMapper;
import com.cyys.admin.auth.mapper.LoginAuditMapper;
import com.cyys.admin.auth.service.LoginAuditService;
import com.cyys.admin.dictionary.service.DictionaryLookupService;
import com.cyys.common.satoken.RequestIdentity;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LoginAuditServiceTest {
    @AfterEach
    void clear() {
        ScopeContext.clear();
    }

    @Test
    void recordsOnlyExplicitAuthenticationFieldsAndPagesCurrentScope() {
        LoginAuditMapper mapper = mock(LoginAuditMapper.class);
        DataAuthorizationMapper authorization = mock(DataAuthorizationMapper.class);
        DictionaryLookupService dictionaries = mock(DictionaryLookupService.class);
        when(authorization.hasPermission("actor", "login-audit:read")).thenReturn(true);
        when(mapper.insert(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(1);
        Page<LoginAuditView> expected = new Page<>(1, 20);
        when(mapper.page(any(), eq("scope-a"), isNull(), eq("FAILURE"))).thenReturn(expected);
        ScopeContext.set(new RequestIdentity("actor", "scope-a", List.of(), List.of()));

        LoginAuditService service = new LoginAuditService(mapper, authorization, dictionaries);
        service.record("alice", "Alice", "u-alice", "scope-a", "127.0.0.1", "SUCCESS", null);
        var actual = service.page(new LoginAuditQuery(1, 20, " ", "FAILURE"));

        verify(mapper).insert(anyString(), eq("alice"), eq("Alice"), eq("u-alice"), eq("scope-a"),
                eq("127.0.0.1"), eq("SUCCESS"), isNull());
        assertThat(actual).isSameAs(expected);
    }

    @Test
    void pageFailsClosedWithoutPermission() {
        LoginAuditMapper mapper = mock(LoginAuditMapper.class);
        DataAuthorizationMapper authorization = mock(DataAuthorizationMapper.class);
        DictionaryLookupService dictionaries = mock(DictionaryLookupService.class);
        ScopeContext.set(new RequestIdentity("actor", "scope-a", List.of(), List.of()));

        assertThatThrownBy(() -> new LoginAuditService(mapper, authorization, dictionaries)
                .page(new LoginAuditQuery(1, 20, null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("登录记录查看权限");
        verifyNoInteractions(mapper);
    }
}
