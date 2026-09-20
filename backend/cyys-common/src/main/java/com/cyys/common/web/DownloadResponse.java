package com.cyys.common.web;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ContentDisposition;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** 私有二进制下载的公共响应约定；授权和流的关闭由调用方负责。 */
public final class DownloadResponse {
    private DownloadResponse() {}
    public static void send(HttpServletResponse response,String name,long bytes,InputStream input) throws IOException {
        response.setContentType("application/octet-stream");
        response.setContentLengthLong(bytes);
        response.setHeader("Content-Disposition",ContentDisposition.attachment().filename(name,StandardCharsets.UTF_8).build().toString());
        response.setHeader("X-Content-Type-Options","nosniff");
        response.setHeader("Cache-Control","no-store");
        input.transferTo(response.getOutputStream());
    }
}
