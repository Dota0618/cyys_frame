package com.cyys.common.mybatis;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 为明确支持的 BaseMapper 方法关联已登记资源；未知方法仍拒绝。 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtectedMapper {
    String value();
}
