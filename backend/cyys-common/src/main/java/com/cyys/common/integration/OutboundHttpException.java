package com.cyys.common.integration;

/** 只携带安全错误分类；不保存外部 URL、请求参数、凭据或响应正文。 */
public final class OutboundHttpException extends RuntimeException {
    private final boolean timeout;

    public OutboundHttpException(String message, boolean timeout) {
        super(message);
        this.timeout = timeout;
    }

    public boolean isTimeout() { return timeout; }
}
