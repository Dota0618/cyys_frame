package com.cyys.common.web;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
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
        return failure(HttpStatus.UNAUTHORIZED.value(), "请重新登录");
    }

    @ExceptionHandler({NotPermissionException.class, NotRoleException.class})
    public ResponseEntity<R<Void>> handleForbidden(Exception error) {
        return failure(HttpStatus.FORBIDDEN.value(), "没有执行此操作的权限");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleValidation(MethodArgumentNotValidException error) {
        return failure(HttpStatus.BAD_REQUEST.value(), error.getBindingResult().getFieldErrors().stream()
                .findFirst().map(e -> e.getDefaultMessage()).orElse("参数校验失败"));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<R<Void>> handleBinding(BindException error) {
        return failure(HttpStatus.BAD_REQUEST.value(), error.getBindingResult().getFieldErrors().stream()
                .findFirst().map(e -> e.getDefaultMessage()).orElse("参数校验失败"));
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<R<Void>> handleConstraint(jakarta.validation.ConstraintViolationException error) {
        return failure(HttpStatus.BAD_REQUEST.value(), error.getConstraintViolations().stream()
                .map(jakarta.validation.ConstraintViolation::getMessage).sorted().findFirst().orElse("参数校验失败"));
    }

    @ExceptionHandler(org.springframework.web.method.annotation.HandlerMethodValidationException.class)
    public ResponseEntity<R<Void>> handleMethodValidation(org.springframework.web.method.annotation.HandlerMethodValidationException error) {
        if (error.isForReturnValue()) return handleUnexpected(error);
        return failure(HttpStatus.BAD_REQUEST.value(), error.getAllErrors().stream()
                .map(org.springframework.context.MessageSourceResolvable::getDefaultMessage)
                .filter(java.util.Objects::nonNull).findFirst().orElse("参数校验失败"));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.web.multipart.support.MissingServletRequestPartException.class})
    public ResponseEntity<R<Void>> handleBadInput(Exception error) {
        return failure(HttpStatus.BAD_REQUEST.value(), "请求参数格式不正确");
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<R<Void>> handleUploadTooLarge(Exception error) { return failure(HttpStatus.PAYLOAD_TOO_LARGE.value(), "文件超过允许大小"); }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<R<Void>> handleUnsupportedContent(Exception error) { return failure(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), "不支持该请求内容格式"); }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<R<Void>> handleUnsupportedResponse(Exception error) { return failure(HttpStatus.NOT_ACCEPTABLE.value(), "不支持该响应内容格式"); }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<R<Void>> handleNotFound(NoResourceFoundException error) {
        return failure(HttpStatus.NOT_FOUND.value(), "接口不存在");
    }

    @ExceptionHandler(org.springframework.dao.ConcurrencyFailureException.class)
    public ResponseEntity<R<Void>> handleConcurrentChange(Exception error) {
        return failure(HttpStatus.CONFLICT.value(),"数据正在被其他操作修改，请刷新后重试");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<R<Void>> handleMethod(HttpRequestMethodNotSupportedException error) {
        return failure(HttpStatus.METHOD_NOT_ALLOWED.value(), "请求方法不支持");
    }

    @ExceptionHandler(com.cyys.common.integration.OutboundHttpException.class)
    public ResponseEntity<R<Void>> handleOutbound(com.cyys.common.integration.OutboundHttpException error) {
        return failure((error.isTimeout() ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_GATEWAY).value(), error.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleUnexpected(Exception error) {
        for (Throwable cause = error.getCause(); cause != null; cause = cause.getCause()) {
            if (cause instanceof ApiException api) return failure(api.getStatus(), api.getMessage());
            if (cause instanceof com.cyys.common.integration.OutboundHttpException outbound) return handleOutbound(outbound);
        }
        log.error("请求处理失败", error);
        return failure(HttpStatus.INTERNAL_SERVER_ERROR.value(), "系统内部错误");
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
        return ResponseEntity.status(status).contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(R.fail(status, message));
    }
}
