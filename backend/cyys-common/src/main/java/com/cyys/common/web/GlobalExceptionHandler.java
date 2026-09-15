package com.cyys.common.web;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<R<Void>> handleApi(ApiException error) {
        return failure(error.getStatus(), error.getMessage());
    }

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<R<Void>> handleNotLogin(NotLoginException error) {
        return failure(401, "请重新登录");
    }

    @ExceptionHandler({NotPermissionException.class, NotRoleException.class})
    public ResponseEntity<R<Void>> handleForbidden(Exception error) {
        return failure(403, "没有执行此操作的权限");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleValidation(MethodArgumentNotValidException error) {
        return failure(400, error.getBindingResult().getFieldErrors().stream()
                .findFirst().map(e -> e.getDefaultMessage()).orElse("参数校验失败"));
    }

    @ExceptionHandler({BindException.class, HttpMessageNotReadableException.class,
            jakarta.validation.ConstraintViolationException.class,
            org.springframework.web.method.annotation.HandlerMethodValidationException.class})
    public ResponseEntity<R<Void>> handleBadInput(Exception error) {
        return failure(400, "请求参数格式不正确");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<R<Void>> handleNotFound(NoResourceFoundException error) {
        return failure(404, "接口不存在");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<R<Void>> handleMethod(HttpRequestMethodNotSupportedException error) {
        return failure(405, "请求方法不支持");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleUnexpected(Exception error) {
        for (Throwable cause = error.getCause(); cause != null; cause = cause.getCause()) {
            if (cause instanceof ApiException api) return failure(api.getStatus(), api.getMessage());
        }
        log.error("请求处理失败", error);
        return failure(500, "系统内部错误");
    }

    private ResponseEntity<R<Void>> failure(int status, String message) {
        var identity = com.cyys.common.satoken.ScopeContext.current();
        var attributes = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (attributes instanceof org.springframework.web.context.request.ServletRequestAttributes servlet) {
            Object route = servlet.getRequest().getAttribute(org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            log.warn("Request rejected: actor={}, scope={}, route={}, method={}, outcome={}",
                    identity.map(com.cyys.common.satoken.RequestIdentity::loginId).orElse("unresolved"),
                    identity.map(com.cyys.common.satoken.RequestIdentity::scopeId).orElse("unselected"),
                    route, servlet.getRequest().getMethod(), status);
        }
        return ResponseEntity.status(status).body(R.fail(status, message));
    }
}
