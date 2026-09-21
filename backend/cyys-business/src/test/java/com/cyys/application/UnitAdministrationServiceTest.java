package com.cyys.application;

import com.cyys.admin.auth.mapper.DataAuthorizationMapper;
import com.cyys.admin.auth.service.UnitAdministrationService;
import com.cyys.admin.user.mapper.SysUserRoleMapper;
import com.cyys.common.satoken.*;
import com.cyys.common.web.ApiException;
import org.junit.jupiter.api.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UnitAdministrationServiceTest {
    private final DataAuthorizationMapper authorization = mock(DataAuthorizationMapper.class);
    private final SysUserRoleMapper roles = mock(SysUserRoleMapper.class);
    private final UnitAdministrationService service = new UnitAdministrationService(authorization, roles);
    @BeforeEach void context() { ScopeContext.set(new RequestIdentity("actor", "unit-a", List.of(), List.of(), List.of())); }
    @AfterEach void clear() { ScopeContext.clear(); }

    @Test void platformAdministratorUsesExplicitPermissionsWithoutUnitMembership() {
        when(roles.selectPermissionCodes("actor", List.of("GLOBAL"))).thenReturn(
                List.of("platform:scope:access", "platform:data:operate", "role:create", "role:grant"));
        assertThat(service.authorize("role:create", true)).contains("role:create", "role:grant");
        verify(roles, never()).selectUnitWidePermissionCodes(any(), any());
    }
    @Test void platformReadOnlyCannotManageUnitAuthorization() {
        when(roles.selectPermissionCodes("actor", List.of("GLOBAL"))).thenReturn(
                List.of("platform:scope:access", "platform:data:read-all", "user:data-permission"));
        assertThatThrownBy(() -> service.authorize("user:data-permission", false)).isInstanceOf(ApiException.class);
    }
    @Test void operationPermissionAloneDoesNotAllowSelfElevation() {
        when(authorization.hasPermission("actor", "unit-a", "user:data-permission")).thenReturn(true);
        when(roles.selectUnitWidePermissionCodes("actor", List.of("unit-a"))).thenReturn(List.of());
        assertThatThrownBy(() -> service.authorize("user:data-permission", false)).isInstanceOf(ApiException.class);
        when(roles.selectUnitWidePermissionCodes("actor", List.of("unit-a"))).thenReturn(List.of("user:data-permission"));
        assertThat(service.authorize("user:data-permission", false)).containsExactly("user:data-permission");
    }
}
