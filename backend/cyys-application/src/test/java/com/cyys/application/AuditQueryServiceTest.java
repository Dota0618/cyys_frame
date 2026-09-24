package com.cyys.application;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.audit.dto.AuditQuery;
import com.cyys.admin.audit.dto.AuditView;
import com.cyys.admin.audit.mapper.DataAuditMapper;
import com.cyys.admin.audit.service.AuditQueryService;
import com.cyys.admin.auth.mapper.DataAuthorizationMapper;
import com.cyys.admin.dictionary.service.DictionaryLookupService;
import com.cyys.common.satoken.RequestIdentity;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuditQueryServiceTest {
    @AfterEach
    void clear() {
        ScopeContext.clear();
    }

    @Test
    void pageUsesCurrentScopeAndNormalizesBlankFilters() {
        DataAuditMapper mapper = mock(DataAuditMapper.class);
        DataAuthorizationMapper authorization = mock(DataAuthorizationMapper.class);
        DictionaryLookupService dictionaries = mock(DictionaryLookupService.class);
        when(authorization.hasPermission("actor", "audit:read")).thenReturn(true);
        Page<AuditView> expected = new Page<>(1, 20);
        when(mapper.page(any(), eq("scope-a"), isNull(), eq("CREATE"), isNull())).thenReturn(expected);
        ScopeContext.set(new RequestIdentity("actor", "scope-a", List.of(), List.of()));

        var actual = new AuditQueryService(mapper, authorization, dictionaries)
                .page(new AuditQuery(1, 20, " ", "CREATE", " "));

        assertThat(actual).isSameAs(expected);
        verify(mapper).page(any(), eq("scope-a"), isNull(), eq("CREATE"), isNull());
    }

    @Test
    void pageFailsClosedWithoutPermission() {
        DataAuditMapper mapper = mock(DataAuditMapper.class);
        DataAuthorizationMapper authorization = mock(DataAuthorizationMapper.class);
        DictionaryLookupService dictionaries = mock(DictionaryLookupService.class);
        ScopeContext.set(new RequestIdentity("actor", "scope-a", List.of(), List.of()));

        assertThatThrownBy(() -> new AuditQueryService(mapper, authorization, dictionaries)
                .page(new AuditQuery(1, 20, null, null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("审计查看权限");
        verifyNoInteractions(mapper);
    }
}
