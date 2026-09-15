package com.cyys.common.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 只允许省略目标单位的 GET 入口；实际跨单位资源权限仍由数据策略逐次验证。 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthorizedScopesRead {}
