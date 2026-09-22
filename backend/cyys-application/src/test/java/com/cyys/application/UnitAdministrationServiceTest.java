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
    @BeforeEach void context() { ScopeContext.set(new RequestIdentity("actor", "unit-a", List.of(), List.of())); }
    @AfterEach void clear() { ScopeContext.clear(); }

    @Test void administrationRequiresBothOperationAndInstitutionRange() {
        when(roles.selectPermissionCodes("actor")).thenReturn(
                List.of("role:create", "role:grant"));
        when(authorization.hasInstitutionWideRange("actor", "unit-a")).thenReturn(true);
        assertThat(service.authorize("role:create", true)).contains("role:create", "role:grant");
        verify(authorization).hasInstitutionWideRange("actor", "unit-a");
    }
    @Test void operationAloneCannotManageInstitutionAuthorization() {
        when(roles.selectPermissionCodes("actor")).thenReturn(
                List.of("user:data-permission"));
        assertThatThrownBy(() -> service.authorize("user:data-permission", false)).isInstanceOf(ApiException.class);
    }
    @Test void operationPermissionAloneDoesNotAllowSelfElevation() {
        when(roles.selectPermissionCodes("actor")).thenReturn(List.of("user:data-permission"));
        when(authorization.hasInstitutionWideRange("actor", "unit-a")).thenReturn(false);
        assertThatThrownBy(() -> service.authorize("user:data-permission", false)).isInstanceOf(ApiException.class);
        when(authorization.hasInstitutionWideRange("actor", "unit-a")).thenReturn(true);
        assertThat(service.authorize("user:data-permission", false)).containsExactly("user:data-permission");
    }
    @Test void institutionAdministratorCannotGrantGlobalOrDescendantRange() {
        var actor = new com.cyys.admin.user.model.SysUserScope();
        actor.setUserId("actor"); actor.setScopeId("unit-a"); actor.setDataRange(1);
        var target = new com.cyys.admin.user.model.SysUserScope();
        target.setScopeId("unit-a"); target.setDataRange(5);
        when(authorization.activeProfile("actor")).thenReturn(actor);
        when(authorization.hasInstitutionWideRange("actor", "unit-a")).thenReturn(true);
        assertThatThrownBy(() -> service.requireAssignableRange(target, 0)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.requireAssignableRange(target, 6)).isInstanceOf(ApiException.class);
        service.requireAssignableRange(target, 1);
        actor.setDataRange(6);
        service.requireAssignableRange(target, 6);
        actor.setDataRange(0);
        service.requireAssignableRange(target, 0);
    }
}
