package com.cyys.application;

import com.cyys.admin.auth.service.PlatformSecurityService;
import com.cyys.admin.auth.service.UnitAdministrationService;
import com.cyys.admin.role.dto.RoleOption;
import com.cyys.admin.role.mapper.RoleGrantMapper;
import com.cyys.admin.role.service.RoleGrantService;
import com.cyys.admin.role.service.RoleService;
import com.cyys.common.satoken.RequestIdentity;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoleGrantOptionsTest {
    private final RoleGrantMapper mapper = mock(RoleGrantMapper.class);
    private final UnitAdministrationService administration = mock(UnitAdministrationService.class);
    private final RoleGrantService service = new RoleGrantService(mapper, administration,
            mock(RoleService.class), mock(PlatformSecurityService.class));

    @BeforeEach void identity() {
        ScopeContext.set(new RequestIdentity("actor", "a", List.of(), List.of()));
        when(administration.authorize("role:grant", true)).thenReturn(Set.of("user:read"));
    }

    @AfterEach void clear() { ScopeContext.clear(); }

    @Test void anyExcessPermissionExcludesRoleWhileRetainingCandidateOrder() {
        var empty = new RoleOption("empty", "无操作权限");
        var reader = new RoleOption("reader", "查询角色");
        var mixed = new RoleOption("mixed", "含越权权限");
        when(mapper.activeRoles()).thenReturn(List.of(empty, mixed, reader));
        when(mapper.permissions(List.of("empty", "mixed", "reader"))).thenReturn(List.of(
                new RoleGrantMapper.GrantPermission("mixed", "user:read"),
                new RoleGrantMapper.GrantPermission("reader", "user:read"),
                new RoleGrantMapper.GrantPermission("mixed", "user:update"),
                new RoleGrantMapper.GrantPermission("mixed", "user:update")));

        assertThat(service.grantableRoleOptions()).containsExactly(empty, reader);
        verify(mapper).activeRoles();
        verify(mapper).permissions(List.of("empty", "mixed", "reader"));
        verifyNoMoreInteractions(mapper);
    }

    @Test void emptyCandidatesDoNotQueryPermissions() {
        when(mapper.activeRoles()).thenReturn(List.of());
        assertThat(service.grantableRoleOptions()).isEmpty();
        verify(mapper, never()).permissions(anyList());
    }

    @Test void deniedAuthorizationPreventsCandidateQueries() {
        when(administration.authorize("role:grant", true)).thenThrow(new ApiException(403, "无授权权限"));
        assertThatThrownBy(service::grantableRoleOptions).hasMessageContaining("无授权权限");
        verifyNoInteractions(mapper);
    }
}
