package com.cyys.common.satoken;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.cyys.common.mybatis.DataPermissionContext;
import com.cyys.common.mybatis.DataPolicyRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class ScopeHandlerInterceptor implements HandlerInterceptor {
    public static final String HEADER_SCOPE_ID = "X-Cyys-Scope-Id";
    public static final String CREDENTIAL_STAMP = "cyys.credential-stamp";
    private final IdentityResolver identityResolver;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        StpUtil.checkLogin();
        RequestIdentity identity = identityResolver.resolve(
                StpUtil.getLoginIdAsString(), request.getHeader(HEADER_SCOPE_ID),
                (String) StpUtil.getTokenSession().get(CREDENTIAL_STAMP));
        boolean globalRead = handler instanceof org.springframework.web.method.HandlerMethod method
                && method.hasMethodAnnotation(com.cyys.common.web.AuthorizedScopesRead.class)
                && "GET".equals(request.getMethod());
        boolean platformControl = handler instanceof org.springframework.web.method.HandlerMethod method
                && method.getBeanType().isAnnotationPresent(com.cyys.common.web.PlatformControl.class);
        boolean accountOperation = handler instanceof org.springframework.web.method.HandlerMethod method
                && method.hasMethodAnnotation(com.cyys.common.web.AccountOperation.class);
        if (!accountOperation && !globalRead && !platformControl) {
            identity.requireScopeId();
        }
        ScopeContext.set(identity);
        if (handler instanceof org.springframework.web.method.HandlerMethod method) {
            SaCheckPermission required = method.getMethodAnnotation(SaCheckPermission.class);
            if (required != null && required.value().length == 1) {
                DataPermissionContext.set(required.value()[0], globalRead
                        ? DataPolicyRegistry.View.AUTHORIZED_SCOPES : DataPolicyRegistry.View.CURRENT_SCOPE);
            }
        }
        return true;
    }
}
