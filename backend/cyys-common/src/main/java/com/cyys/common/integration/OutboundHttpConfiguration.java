package com.cyys.common.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.HttpCookieHandling;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** 保留 Boot 的客户端、SSL Bundle 和 Jackson 配置，只补统一传输边界。 */
@Configuration(proxyBeanMethods = false)
public class OutboundHttpConfiguration {
    @Bean
    RestClientCustomizer outboundHttpPolicy(HttpClientSettings settings,
            @Value("${cyys.http.max-response-bytes:1048576}") int maxBytes) {
        requireTimeout(settings.connectTimeout());
        requireTimeout(settings.readTimeout());
        if (settings.redirects() != HttpRedirects.DONT_FOLLOW
                || settings.cookieHandling() != HttpCookieHandling.DISABLE) {
            throw new IllegalArgumentException("Outbound HTTP must disable redirects and shared cookies");
        }
        if (maxBytes < 1 || maxBytes > 16 * 1024 * 1024) {
            throw new IllegalArgumentException("Outbound HTTP response limit must be between 1 byte and 16 MiB");
        }
        return builder -> builder.configureMessageConverters(converters -> converters.configureMessageConverters(converter -> {
            if (converter instanceof StringHttpMessageConverter text) text.setDefaultCharset(StandardCharsets.UTF_8);
            if (converter instanceof FormHttpMessageConverter form) form.setCharset(StandardCharsets.UTF_8);
        })).requestInterceptor((request, body, execution) -> {
            MediaType type = request.getHeaders().getContentType();
            if (type != null && type.getCharset() == null && ("text".equals(type.getType())
                    || MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(type))) {
                request.getHeaders().setContentType(new MediaType(type, StandardCharsets.UTF_8));
            }
            long deadline = System.nanoTime() + settings.readTimeout().toNanos();
            ClientHttpResponse response;
            try { response = execution.execute(request, body); }
            catch (IOException error) { throw transportFailure(error, deadline); }
            try {
                // 放在拦截器内，retrieve 与 exchange 都不能把失败响应伪装成业务成功。
                if (!response.getStatusCode().is2xxSuccessful()) {
                    throw new OutboundHttpException("外部服务返回异常状态（" + response.getStatusCode().value() + "）", false);
                }
                if (response.getHeaders().getContentLength() > maxBytes) throw tooLarge();
                return new LimitedResponse(response, maxBytes, deadline);
            } catch (IOException error) {
                response.close(); throw transportFailure(error, deadline);
            } catch (RuntimeException error) {
                response.close(); throw error;
            }
        });
    }

    private static void requireTimeout(Duration timeout) {
        if (timeout == null || timeout.compareTo(Duration.ofMillis(1)) < 0 || timeout.compareTo(Duration.ofMinutes(2)) > 0) {
            throw new IllegalArgumentException("Outbound HTTP timeouts must be between 1 ms and 2 minutes");
        }
    }

    private static OutboundHttpException tooLarge() { return new OutboundHttpException("外部服务响应超过允许大小", false); }
    private static OutboundHttpException transportFailure(IOException error, long deadline) {
        // JDK 工厂在响应体超时后关闭流，可能只抛 IOException；按同一请求期限判定。
        boolean timeout = System.nanoTime() - deadline >= 0;
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException) { timeout = true; break; }
        }
        return new OutboundHttpException(timeout ? "外部服务请求超时" : "外部服务连接失败", timeout);
    }

    private static final class LimitedResponse implements ClientHttpResponse {
        private final ClientHttpResponse response;
        private final int maxBytes;
        private final long deadline;
        private InputStream body;
        private LimitedResponse(ClientHttpResponse response, int maxBytes, long deadline) {
            this.response = response; this.maxBytes = maxBytes; this.deadline = deadline;
        }
        public HttpStatusCode getStatusCode() throws IOException { return response.getStatusCode(); }
        public String getStatusText() throws IOException { return response.getStatusText(); }
        public HttpHeaders getHeaders() { return response.getHeaders(); }
        public void close() { response.close(); }
        public InputStream getBody() throws IOException {
            if (body == null) body = new FilterInputStream(response.getBody()) {
                private int count;
                public int read() {
                    try {
                        int value = in.read();
                        if (value != -1 && ++count > maxBytes) throw tooLarge();
                        return value;
                    } catch (IOException error) { throw transportFailure(error, deadline); }
                }
                public int read(byte[] buffer, int offset, int length) {
                    if (length == 0) return 0;
                    try {
                        int size = in.read(buffer, offset, Math.min(length, maxBytes - count + 1));
                        if (size > 0 && (count += size) > maxBytes) throw tooLarge();
                        return size;
                    } catch (IOException error) { throw transportFailure(error, deadline); }
                }
                public long skip(long size) throws IOException {
                    if (size <= 0) return 0;
                    byte[] buffer = new byte[(int) Math.min(4096, size)];
                    long skipped = 0;
                    while (skipped < size) {
                        int length = read(buffer, 0, (int) Math.min(buffer.length, size - skipped));
                        if (length == -1) break;
                        skipped += length;
                    }
                    return skipped;
                }
                public boolean markSupported() { return false; }
                public void mark(int readLimit) { }
                public void reset() throws IOException { throw new IOException("Response stream cannot reset"); }
            };
            return body;
        }
    }
}
