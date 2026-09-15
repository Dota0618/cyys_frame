package com.cyys.common.satoken;

import cn.dev33.satoken.stp.StpUtil;
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
        if (!"/api/auth/me".equals(request.getServletPath()) && !globalRead) {
            identity.requireScopeId();
        }
        ScopeContext.set(identity);
        return true;
    }
}
