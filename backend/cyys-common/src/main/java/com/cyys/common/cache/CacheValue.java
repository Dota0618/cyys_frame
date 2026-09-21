package com.cyys.common.cache;

import java.lang.annotation.*;

/** 仅用于已授权响应 DTO；省略 field 取整条记录，列表来源省略 key/field 取完整列表。 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.RECORD_COMPONENT, ElementType.FIELD})
public @interface CacheValue {
    String cache();
    String key() default "";
    String field() default "";
    String variant() default "";
    /** 当前 DTO 内的实际归属字段名；单位来源省略时使用当前已授权单位。 */
    String scopeKey() default "";
}
