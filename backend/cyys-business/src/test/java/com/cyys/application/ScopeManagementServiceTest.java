package com.cyys.application;

import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.auth.service.PlatformSecurityService;
import com.cyys.admin.scope.dto.*;
import com.cyys.admin.scope.mapper.ScopeManagementMapper;
import com.cyys.admin.scope.model.SysScope;
import com.cyys.admin.scope.service.ScopeManagementService;
import com.cyys.common.satoken.*;
import com.cyys.common.web.ApiException;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScopeManagementServiceTest {
    private final PlatformSecurityService security=mock(PlatformSecurityService.class);
    private final ScopeManagementMapper mapper=mock(ScopeManagementMapper.class);
    private final DataAuditService audit=mock(DataAuditService.class);
    private final ScopeManagementService service=new ScopeManagementService(security,mapper,audit,new DataIsolationProperties(DataIsolationProperties.Mode.MULTI,null));
    @BeforeEach void identity() { ScopeContext.set(new RequestIdentity("actor",null,List.of(),List.of(),List.of())); }
    @AfterEach void clear() { ScopeContext.clear(); }
    private CreateScopeCommand command(String code) { return new CreateScopeCommand(code,"测试单位",null,null,null,null); }
    private SysScope row() { var row=new SysScope();row.setId("scope-a");row.setCode("UNIT_A");row.setName("单位");row.setStatus(1);row.setSort(0);row.setVersion(0);return row; }

    @Test void rejectedPlatformPermissionNeverTouchesRegistry() {
        when(security.authorize("platform:scope:create")).thenThrow(new ApiException(403,"denied"));
        assertThatThrownBy(()->service.create(command("NEW"))).isInstanceOf(ApiException.class);
        verifyNoInteractions(mapper,audit);
    }
    @Test void createDefaultsAndReservedCode() {
        when(mapper.create(any(),eq("actor"))).thenReturn(1);
        var result=service.create(command("NEW"));
        assertThat(result.enabled()).isTrue();assertThat(result.version()).isZero();assertThat(result.sort()).isZero();
        verify(audit).platformSuccess("scope",result.id(),"CREATE");
        assertThatThrownBy(()->service.create(command("GLOBAL"))).isInstanceOf(ApiException.class);
    }
    @Test void conflictingVersionAndFixedUnitCannotBeDisabled() {
        when(mapper.lock("scope-a")).thenReturn(row());
        var stale=new UpdateScopeCommand(1,"单位",null,null,null,true,0,null);
        assertThatThrownBy(()->service.update("scope-a",stale)).isInstanceOf(ApiException.class).hasMessageContaining("修改");
        var single=new ScopeManagementService(security,mapper,audit,new DataIsolationProperties(DataIsolationProperties.Mode.SINGLE,"scope-a"));
        assertThatThrownBy(()->single.update("scope-a",new UpdateScopeCommand(0,"单位",null,null,null,false,0,null)))
                .isInstanceOf(ApiException.class).hasMessageContaining("固定单位");
        verify(mapper,never()).update(any(),any());verifyNoInteractions(audit);
    }
    @Test void updateUsesVersionAndWritesAudit() {
        when(mapper.lock("scope-a")).thenReturn(row());when(mapper.update(any(),eq("actor"))).thenReturn(1);
        var result=service.update("scope-a",new UpdateScopeCommand(0,"单位新名称",null,null,null,false,2,null));
        assertThat(result.enabled()).isFalse();assertThat(result.version()).isEqualTo(1);assertThat(result.code()).isEqualTo("UNIT_A");
        verify(audit).platformSuccess("scope","scope-a","UPDATE");
    }
}
