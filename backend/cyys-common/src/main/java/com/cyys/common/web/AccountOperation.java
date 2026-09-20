package com.cyys.common.web;

import java.lang.annotation.*;

/** 仅用于当前账号自身操作；允许未选择单位，不授予平台权限或单位数据访问权。 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AccountOperation {}
