package com.cyys.application;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.cyys.admin.generator.controller.GeneratorController;
import com.cyys.admin.generator.dto.GenerateCodeCommand;
import com.cyys.admin.generator.service.GeneratorService;
import com.cyys.admin.notice.controller.SystemNoticeController;
import com.cyys.admin.notice.dto.CreateSystemNoticeCommand;
import com.cyys.admin.notice.service.SystemNoticeService;
import com.cyys.admin.release.controller.ReleaseController;
import com.cyys.admin.release.service.ReleaseService;
import com.cyys.admin.scope.controller.ScopeManagementController;
import com.cyys.admin.scope.dto.CreateScopeCommand;
import com.cyys.admin.scope.service.ScopeManagementService;
import com.cyys.common.mybatis.DataPermissionContext;
import com.cyys.common.mybatis.DataPolicyRegistry;
import com.cyys.common.satoken.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 验证真实 Sa-Token 注解入口，不启动数据库或访问业务数据。 */
class PermissionBoundaryTest {
    @AfterEach void clear() { DataPermissionContext.clear(); ScopeContext.clear(); }

    @Test void readDoesNotGrantWritesAndDeniedEntryNeverCallsService() throws Exception {
        var service = mock(SystemNoticeService.class);
        var controller = new SystemNoticeController(service);
        assertThat(check(controller, "detail", List.of("system-notice:read"), String.class)).isTrue();
        assertThatThrownBy(() -> check(controller, "create", List.of("system-notice:read"), CreateSystemNoticeCommand.class))
                .isInstanceOf(NotPermissionException.class);
        assertThatThrownBy(() -> check(controller, "delete", List.of("system-notice:read"), String.class, int.class))
                .isInstanceOf(NotPermissionException.class);
        verifyNoInteractions(service);
    }

    @Test void sharedOptionsAcceptCreateOrUpdateButNotReadAlone() throws Exception {
        var controller = new SystemNoticeController(mock(SystemNoticeService.class));
        for (String permission : List.of("system-notice:create", "system-notice:update"))
            assertThat(check(controller, "options", List.of(permission), String.class)).isTrue();
        assertThatThrownBy(() -> check(controller, "options", List.of("system-notice:read"), String.class))
                .isInstanceOf(NotPermissionException.class);
    }

    @Test void fileSelectionRequiresAllDeclaredPermissions() throws Exception {
        var controller = new ReleaseController(mock(ReleaseService.class));
        assertThatThrownBy(() -> check(controller, "createFiles", List.of("release:create"), int.class, int.class, String.class))
                .isInstanceOf(NotPermissionException.class);
        assertThat(check(controller, "createFiles", List.of("release:create", "file:read", "file:content:read"),
                int.class, int.class, String.class)).isTrue();
    }

    @Test void scopeAndGeneratorPermissionsRemainAtTheirEntrypoints() throws Exception {
        var scopes = new ScopeManagementController(mock(ScopeManagementService.class));
        assertThatThrownBy(() -> check(scopes, "create", List.of(), CreateScopeCommand.class))
                .isInstanceOf(NotPermissionException.class);
        var generator = new GeneratorController(mock(GeneratorService.class));
        assertThat(check(generator, "preview", List.of("generator:read"), GenerateCodeCommand.class)).isTrue();
        assertThatThrownBy(() -> check(generator, "download", List.of("generator:read"), GenerateCodeCommand.class,
                jakarta.servlet.http.HttpServletResponse.class)).isInstanceOf(NotPermissionException.class);
    }

    @Test void classPermissionWorksWithoutMethodPermission() throws Exception {
        assertThat(check(new ClassPermissionController(), "list", List.of("example:read"))).isTrue();
        assertThatThrownBy(() -> check(new ClassPermissionController(), "list", List.of()))
                .isInstanceOf(NotPermissionException.class);
    }

    @Test void dataContextIsEstablishedForClassAndMultiplePermissions() throws Exception {
        var resolver = mock(IdentityResolver.class);
        when(resolver.resolve(any(), any(), any())).thenReturn(new RequestIdentity("actor", "a", List.of(), List.of()));
        var interceptor = new ScopeHandlerInterceptor(resolver);
        var request = new MockHttpServletRequest("GET", "/options");
        request.addHeader(ScopeHandlerInterceptor.HEADER_SCOPE_ID, "a");
        var session = new SaSession("test"); session.set(ScopeHandlerInterceptor.CREDENTIAL_STAMP, "stamp");
        try (var token = mockStatic(StpUtil.class)) {
            token.when(StpUtil::getLoginIdAsString).thenReturn("actor");
            token.when(StpUtil::getTokenSession).thenReturn(session);
            for (HandlerMethod handler : List.of(
                    new HandlerMethod(new ClassPermissionController(), "list"),
                    new HandlerMethod(new SystemNoticeController(mock(SystemNoticeService.class)), "options", String.class))) {
                assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), handler)).isTrue();
                assertThat(DataPermissionContext.get().view()).isEqualTo(DataPolicyRegistry.View.CURRENT_SCOPE);
                clear();
            }
        }
    }

    private boolean check(Object controller, String method, List<String> permissions, Class<?>... parameters) throws Exception {
        var logic = spy(new StpLogic("login"));
        doReturn("actor").when(logic).getLoginId();
        doReturn(permissions).when(logic).getPermissionList("actor");
        try (var manager = mockStatic(SaManager.class, CALLS_REAL_METHODS)) {
            manager.when(() -> SaManager.getStpLogic("", false)).thenReturn(logic);
            return new SaInterceptor(handler -> {}).preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(),
                    new HandlerMethod(controller, method, parameters));
        }
    }

    @SaCheckPermission("example:read")
    static class ClassPermissionController { public void list() {} }
}
