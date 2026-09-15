package com.cyys.common.web;

import lombok.Data;
import org.springframework.http.HttpStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class R<T> implements Serializable {

    private Integer code;
    private String msg;
    private T data;
    private LocalDateTime timestamp;

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(HttpStatus.OK.value());
        r.setMsg("success");
        r.setData(data);
        r.setTimestamp(LocalDateTime.now());
        return r;
    }

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> fail(String msg) {
        return fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), msg);
    }

    public static <T> R<T> fail() {
        return fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), "操作失败");
    }

    public static <T> R<T> fail(Integer code, String msg) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMsg(msg);
        r.setTimestamp(LocalDateTime.now());
        return r;
    }
}
