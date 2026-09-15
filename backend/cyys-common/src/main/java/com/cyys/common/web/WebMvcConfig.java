package com.cyys.common.web;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.cyys.common.satoken.DataIsolationProperties;
import com.cyys.common.satoken.ScopeHandlerInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(DataIsolationProperties.class)
public class WebMvcConfig implements WebMvcConfigurer {
    private static final String[] PUBLIC_PATHS = {
            "/api/auth/login", "/api/health", "/error", "/swagger-ui/**", "/v3/api-docs/**"
    };
    private final ScopeHandlerInterceptor scopeHandlerInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(scopeHandlerInterceptor).addPathPatterns("/**")
                .excludePathPatterns(PUBLIC_PATHS).excludePathPatterns("/api/auth/logout").order(0);
        // 先建立可信上下文，再执行 Sa-Token 注解中的权限检查。
        registry.addInterceptor(new SaInterceptor(handler -> StpUtil.checkLogin()))
                .addPathPatterns("/**").excludePathPatterns(PUBLIC_PATHS).order(1);
    }
}
