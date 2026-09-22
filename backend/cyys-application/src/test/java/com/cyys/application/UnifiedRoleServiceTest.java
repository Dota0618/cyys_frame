package com.cyys.application;

import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.auth.service.PlatformSecurityService;
import com.cyys.admin.role.dto.*;
import com.cyys.admin.role.mapper.RoleMapper;
import com.cyys.admin.role.model.SysRole;
import com.cyys.admin.role.service.RoleService;
import com.cyys.common.satoken.*;
import com.cyys.common.web.ApiException;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UnifiedRoleServiceTest {
    private final RoleMapper mapper = mock(RoleMapper.class);
    private final PlatformSecurityService security = mock(PlatformSecurityService.class);
    private final DataAuditService audit = mock(DataAuditService.class);
    private final RoleService service = new RoleService(mapper, security, audit);

    @BeforeEach void identity() {
        ScopeContext.set(new RequestIdentity("actor", "institution-a", List.of(), List.of()));
        when(security.permissionCeiling()).thenReturn(Set.of("user:read", "role:grant"));
        when(mapper.lockAccount("target")).thenReturn(new AccountRow("target", "target", "用户", 1));
    }
    @AfterEach void clear() { ScopeContext.clear(); }

    @Test void cannotReplaceRolesOfMorePrivilegedAccount() {
        when(mapper.accountRoleIds("target")).thenReturn(List.of("administrator"));
        when(mapper.permissions(List.of("administrator"))).thenReturn(List.of(new RolePermission("menu:update")));
        assertThatThrownBy(() -> service.assign("target", new AssignRolesCommand(List.of())))
                .isInstanceOf(ApiException.class).hasMessageContaining("现有权限");
        verify(mapper, never()).removeAccountRoles(any());
    }

    @Test void grantingUsesOneAccountRoleSetAndChecksLastAdministrator() {
        when(mapper.accountRoleIds("target")).thenReturn(List.of());
        when(mapper.lockRoles(List.of("reader"))).thenReturn(List.of(new SysRole()));
        when(mapper.permissions(List.of("reader"))).thenReturn(List.of(new RolePermission("user:read")));
        when(mapper.addAccountRole(any(), eq("target"), eq("reader"), eq("actor"))).thenReturn(1);
        service.assign("target", new AssignRolesCommand(List.of("reader")));
        var order = inOrder(security, mapper, audit);
        order.verify(security).lockAdministratorGuard();
        order.verify(mapper).lockAccount("target");
        order.verify(mapper).removeAccountRoles("target");
        order.verify(mapper).addAccountRole(any(), eq("target"), eq("reader"), eq("actor"));
        order.verify(security).requireUsableAdministrator();
        order.verify(audit).platformSuccess("account-role", "target", "GRANT");
    }

    @Test void cannotGrantMissingOrExcessPermissions() {
        when(mapper.lockRoles(List.of("invalid"))).thenReturn(List.of());
        assertThatThrownBy(() -> service.assign("target", new AssignRolesCommand(List.of("invalid"))))
                .isInstanceOf(ApiException.class);
        when(mapper.lockRoles(List.of("powerful"))).thenReturn(List.of(new SysRole()));
        when(mapper.permissions(List.of("powerful"))).thenReturn(List.of(new RolePermission("menu:update")));
        assertThatThrownBy(() -> service.assign("target", new AssignRolesCommand(List.of("powerful"))))
                .isInstanceOf(ApiException.class);
        verify(mapper, never()).removeAccountRoles(any());
    }
}
