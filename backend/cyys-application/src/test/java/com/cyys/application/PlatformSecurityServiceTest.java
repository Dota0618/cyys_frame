package com.cyys.application;

import com.cyys.admin.auth.mapper.DataAuthorizationMapper;
import com.cyys.admin.auth.mapper.PlatformSecurityMapper;
import com.cyys.admin.auth.service.PlatformSecurityService;
import com.cyys.common.satoken.RequestIdentity;
import com.cyys.common.satoken.ScopeContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PlatformSecurityServiceTest {
    private final DataAuthorizationMapper authorization = mock(DataAuthorizationMapper.class);
    private final PlatformSecurityMapper mapper = mock(PlatformSecurityMapper.class);
    private final PlatformSecurityService service = new PlatformSecurityService(authorization, mapper);

    @BeforeEach
    void identity() {
        ScopeContext.set(new RequestIdentity("actor", "unit-a", List.of(), List.of()));
    }

    @AfterEach
    void clear() {
        ScopeContext.clear();
    }

    @Test
    void superAdminCeilingContainsAllActivePermissions() {
        ScopeContext.clear();
        ScopeContext.set(new RequestIdentity("actor", "unit-a",
                List.of(PlatformSecurityService.SUPER_ADMIN), List.of("role:grant")));
        when(authorization.activePermissionCodes()).thenReturn(List.of("recycle:read", "recycle:purge"));

        assertThat(service.permissionCeiling()).containsExactlyInAnyOrder("recycle:read", "recycle:purge");
        verify(authorization).activePermissionCodes();
        verify(authorization, never()).permissions(anyString());
    }

    @Test
    void regularAccountCeilingUsesGrantedPermissions() {
        when(authorization.permissions("actor")).thenReturn(List.of("role:grant"));

        assertThat(service.permissionCeiling()).containsExactly("role:grant");
        verify(authorization).permissions("actor");
        verify(authorization, never()).activePermissionCodes();
    }
}
