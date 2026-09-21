package com.cyys.application;

import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.auth.service.UnitAdministrationService;
import com.cyys.admin.user.dto.UpdateMemberDataPermissionCommand;
import com.cyys.admin.user.mapper.MemberDataPermissionMapper;
import com.cyys.admin.user.model.SysUserScope;
import com.cyys.admin.user.service.MemberDataPermissionService;
import com.cyys.common.satoken.*;
import com.cyys.common.web.ApiException;
import org.junit.jupiter.api.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MemberDataPermissionServiceTest {
    private final MemberDataPermissionMapper mapper = mock(MemberDataPermissionMapper.class);
    private final UnitAdministrationService administration = mock(UnitAdministrationService.class);
    private final DataAuditService audit = mock(DataAuditService.class);
    private final MemberDataPermissionService service = new MemberDataPermissionService(mapper, administration, audit, mock(com.cyys.admin.auth.service.PlatformSecurityService.class));
    @BeforeEach void context() {
        ScopeContext.set(new RequestIdentity("actor", "unit-a", List.of(), List.of()));
        var member = new SysUserScope(); member.setId("member"); member.setUserId("target"); member.setScopeId("unit-a");
        when(mapper.lockMembers("member", "unit-a", "actor")).thenReturn(List.of(member));
    }
    @AfterEach void clear() { ScopeContext.clear(); }

    @Test void rejectsUnauthorizedActorBeforeReadingOrWritingMembers() {
        when(administration.authorize("user:data-permission", false)).thenThrow(new ApiException(403, "denied"));
        assertThatThrownBy(() -> service.update("member", new UpdateMemberDataPermissionCommand(0, 1, List.of())))
                .isInstanceOf(ApiException.class);
        verifyNoInteractions(mapper, audit);
    }
    @Test void rechecksAdministratorAfterAcquiringLocks() {
        when(administration.authorize("user:data-permission", false)).thenReturn(java.util.Set.of())
                .thenThrow(new ApiException(403, "revoked"));
        assertThatThrownBy(() -> service.update("member", new UpdateMemberDataPermissionCommand(0, 1, List.of())))
                .hasMessage("revoked");
        verify(mapper, never()).update(any(), any(), anyInt(), anyInt(), any());
    }
    @Test void rejectsForeignDepartmentsAndStaleVersionWithoutWrites() {
        when(mapper.lockDepartments(List.of("foreign"), "unit-a")).thenReturn(List.of());
        assertThatThrownBy(() -> service.update("member", new UpdateMemberDataPermissionCommand(0, 2, List.of("foreign"))))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(403));
        assertThatThrownBy(() -> service.update("member", new UpdateMemberDataPermissionCommand(9, 5, List.of())))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(409));
        verify(mapper, never()).update(any(), any(), anyInt(), anyInt(), any());
        verifyNoInteractions(audit);
    }
    @Test void rejectsInvalidDepartmentShapes() {
        for (var command : List.of(new UpdateMemberDataPermissionCommand(0, 2, List.of()),
                new UpdateMemberDataPermissionCommand(0, 2, List.of("a", "a")),
                new UpdateMemberDataPermissionCommand(0, 5, List.of("a")))) {
            assertThatThrownBy(() -> service.update("member", command)).isInstanceOf(ApiException.class);
        }
        verify(mapper, never()).update(any(), any(), anyInt(), anyInt(), any());
    }
    @Test void savesMemberRangeAndClearsPreviousCustomDepartmentsWithAudit() {
        when(mapper.update("member", "unit-a", 0, 5, "actor")).thenReturn(1);
        var view = service.update("member", new UpdateMemberDataPermissionCommand(0, 5, List.of()));
        assertThat(view.dataRange()).isEqualTo(5); assertThat(view.version()).isEqualTo(1);
        verify(mapper).removeDepartments("member", "unit-a");
        verify(audit).success("user-data-permission", "member", "UPDATE");
    }
}
