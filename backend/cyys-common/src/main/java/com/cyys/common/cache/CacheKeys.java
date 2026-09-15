package com.cyys.common.cache;

import com.cyys.common.satoken.ScopeContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class CacheKeys {
    private final String prefix;

    public CacheKeys(@Value("${spring.application.name}") String application,
                     @Value("${cyys.environment:local}") String environment) {
        this.prefix = segment(application) + ":" + segment(environment);
    }

    public String unit(String feature) {
        return prefix + ":unit:" + segment(ScopeContext.get().requireScopeId()) + ":" + segment(feature);
    }

    public String platform(String feature) { return prefix + ":platform:" + segment(feature); }
    public String sessionPrefix() { return prefix + ":session:"; }

    private static String segment(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("缓存命名空间不能为空");
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
