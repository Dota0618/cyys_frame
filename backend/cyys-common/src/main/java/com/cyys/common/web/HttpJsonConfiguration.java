package com.cyys.common.web;

import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;

import java.util.List;

/** Swagger 内部仍使用 Jackson 2；不得因其 YAML 依赖扩展应用的 HTTP 格式契约。 */
@Configuration(proxyBeanMethods = false)
public class HttpJsonConfiguration {
    @Bean
    ServerHttpMessageConvertersCustomizer serverJsonBoundary() {
        return builder -> builder.configureMessageConvertersList(HttpJsonConfiguration::removeLegacyConverters);
    }

    @Bean
    ClientHttpMessageConvertersCustomizer clientJsonBoundary() {
        return builder -> builder.configureMessageConvertersList(HttpJsonConfiguration::removeLegacyConverters);
    }

    @SuppressWarnings("removal")
    private static void removeLegacyConverters(List<HttpMessageConverter<?>> converters) {
        converters.removeIf(converter -> converter instanceof AbstractJackson2HttpMessageConverter);
    }
}
