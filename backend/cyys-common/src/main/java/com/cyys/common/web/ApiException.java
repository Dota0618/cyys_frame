package com.cyys.common.web;

/** 可预期的请求失败；HTTP 状态由统一异常处理器输出。 */
public class ApiException extends RuntimeException {
    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
