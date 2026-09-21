package com.cyys.application;

import com.cyys.admin.auth.dto.ScopeInfo;
import com.cyys.admin.auth.service.IdentityService;
import com.cyys.admin.scope.mapper.SysScopeMapper;
import com.cyys.admin.user.mapper.*;
import com.cyys.admin.user.model.SysUser;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UnifiedRoleIdentityTest {
    @Test void institutionFilterDoesNotChangeRolesOrPermissions() {
        var accounts = mock(SysUserMapper.class);
        var profiles = mock(SysUserScopeMapper.class);
        var scopes = mock(SysScopeMapper.class);
        var roles = mock(SysUserRoleMapper.class);
        var service = new IdentityService(accounts, profiles, roles);
        var user = new SysUser(); user.setId("user");
        var profile = new com.cyys.admin.user.model.SysUserScope(); profile.setUserId("user"); profile.setScopeId("a"); profile.setDataRange(6);
        when(profiles.profile("user")).thenReturn(profile);
        when(profiles.selectAvailableScopes("user")).thenReturn(List.of(
                new ScopeInfo("a", "A", "机构 A", true, "0"),
                new ScopeInfo("b", "B", "机构 B", false, "a")));
        when(roles.selectRoleCodes("user")).thenReturn(List.of("READER"));
        when(roles.selectPermissionCodes("user")).thenReturn(List.of("user:read", "file:read"));
        var first = service.forLogin(user, "a").identity();
        var second = service.forLogin(user, "b").identity();
        assertThat(first.scopeId()).isEqualTo("a");
        assertThat(second.scopeId()).isEqualTo("b");
        assertThat(second.roles()).isEqualTo(first.roles());
        assertThat(second.permissions()).isEqualTo(first.permissions());
        verify(roles, times(2)).selectPermissionCodes("user");
    }
}
