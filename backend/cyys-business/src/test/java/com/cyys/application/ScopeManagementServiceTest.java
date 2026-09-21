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
    private final ScopeManagementService service=new ScopeManagementService(security,mapper,audit);
    @BeforeEach void identity() { ScopeContext.set(new RequestIdentity("actor",null,List.of(),List.of())); when(mapper.lockStructure()).thenReturn("scope-update"); }
    @AfterEach void clear() { ScopeContext.clear(); }
    private CreateScopeCommand command(String code) { return new CreateScopeCommand(code,"测试单位",null,null,null,null,"0"); }
    private SysScope row() { var row=new SysScope();row.setId("scope-a");row.setParentId("0");row.setAncestors("");row.setCode("UNIT_A");row.setName("单位");row.setStatus(1);row.setSort(0);row.setVersion(0);return row; }

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
        verify(mapper).attachSystemAccounts(result.id(),"actor");
        assertThatThrownBy(()->service.create(command("GLOBAL"))).isInstanceOf(ApiException.class);
    }
    @Test void conflictingVersionCannotBeUpdated() {
        when(mapper.lockTree()).thenReturn(List.of(row()));
        var stale=new UpdateScopeCommand(1,"单位",null,null,null,true,0,null,"0");
        assertThatThrownBy(()->service.update("scope-a",stale)).isInstanceOf(ApiException.class).hasMessageContaining("修改");
        verify(mapper,never()).update(any(),any());verifyNoInteractions(audit);
    }
    @Test void updateUsesVersionAndWritesAudit() {
        when(mapper.lockTree()).thenReturn(List.of(row()));when(mapper.update(any(),eq("actor"))).thenReturn(1);
        var result=service.update("scope-a",new UpdateScopeCommand(0,"单位新名称",null,null,null,true,2,null,"0"));
        assertThat(result.enabled()).isTrue();assertThat(result.version()).isEqualTo(1);assertThat(result.code()).isEqualTo("UNIT_A");
        verify(audit).platformSuccess("scope","scope-a","UPDATE");
    }

    @Test void createChildUsesParentPathAndRejectsInactiveParent() {
        var parent=row();when(mapper.lockTree()).thenReturn(List.of(parent));
        when(mapper.create(any(),eq("actor"))).thenReturn(1);
        var child=service.create(new CreateScopeCommand("CHILD","下属机构",null,null,null,null,"scope-a"));
        assertThat(child.parentId()).isEqualTo("scope-a");assertThat(child.ancestors()).isEqualTo("scope-a");
        parent.setStatus(0);
        assertThatThrownBy(()->service.create(new CreateScopeCommand("CHILD2","下属机构",null,null,null,null,"scope-a")))
                .isInstanceOf(ApiException.class).hasMessageContaining("停用");
    }

    @Test void moveRejectsCyclesAndUpdatesAllDescendantPaths() {
        var main=row();main.setId("main");
        var root=row();root.setParentId("main");root.setAncestors("main");
        var child=row();child.setId("child");child.setParentId("scope-a");child.setAncestors("main,scope-a");
        var target=row();target.setId("target");target.setParentId("main");target.setAncestors("main");
        when(mapper.lockTree()).thenReturn(List.of(main,root,child,target));
        assertThatThrownBy(()->service.update("scope-a",new UpdateScopeCommand(0,"机构",null,null,null,true,0,null,"child")))
                .isInstanceOf(ApiException.class).hasMessageContaining("下级");
        verify(mapper,never()).updateAncestors(any(),any(),any());
        when(mapper.update(any(),eq("actor"))).thenReturn(1);
        when(mapper.updateAncestors("child","main,target,scope-a","actor")).thenReturn(1);
        var result=service.update("scope-a",new UpdateScopeCommand(0,"机构",null,null,null,true,0,null,"target"));
        assertThat(result.ancestors()).isEqualTo("main,target");
        verify(mapper).updateAncestors("child","main,target,scope-a","actor");
    }

    @Test void rootCannotBeDisabledMovedOrDuplicatedAndChildCannotBecomeRoot() {
        var root=row();var child=row();child.setId("child");child.setParentId("scope-a");child.setAncestors("scope-a");
        when(mapper.lockTree()).thenReturn(List.of(root,child));
        assertThatThrownBy(()->service.update("scope-a",new UpdateScopeCommand(0,"机构",null,null,null,false,0,null,"0")))
                .isInstanceOf(ApiException.class).hasMessageContaining("总单位");
        assertThatThrownBy(()->service.update("scope-a",new UpdateScopeCommand(0,"机构",null,null,null,true,0,null,"child")))
                .isInstanceOf(ApiException.class).hasMessageContaining("总单位");
        assertThatThrownBy(()->service.update("child",new UpdateScopeCommand(0,"机构",null,null,null,true,0,null,"0")))
                .isInstanceOf(ApiException.class).hasMessageContaining("总单位");
        assertThatThrownBy(()->service.create(command("OTHER_ROOT")))
                .isInstanceOf(ApiException.class).hasMessageContaining("总单位");
        verify(mapper,never()).create(any(),any());
        verify(mapper,never()).update(any(),any());
    }
}
