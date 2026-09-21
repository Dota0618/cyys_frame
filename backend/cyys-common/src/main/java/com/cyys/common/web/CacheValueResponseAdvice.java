package com.cyys.common.web;

import com.cyys.common.cache.CachedResponseValues;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.*;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/** 仅处理业务成功响应，权限拒绝、下载和流式输出不触发名称加载。 */
@RestControllerAdvice
@RequiredArgsConstructor
public class CacheValueResponseAdvice implements ResponseBodyAdvice<Object> {
    private final CachedResponseValues values;
    @Override public boolean supports(MethodParameter method, Class<? extends HttpMessageConverter<?>> converter) { return true; }
    @Override public Object beforeBodyWrite(Object body, MethodParameter method, MediaType media,
                                           Class<? extends HttpMessageConverter<?>> converter,
                                           ServerHttpRequest request, ServerHttpResponse response) {
        return body instanceof R<?> result && Integer.valueOf(200).equals(result.getCode()) ? values.enrich(body) : body;
    }
}
