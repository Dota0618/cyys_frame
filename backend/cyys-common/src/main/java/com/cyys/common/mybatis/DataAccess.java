package com.cyys.common.mybatis;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 自定义 Mapper 语句的资源、操作及视图；不是忽略 SQL 隔离的开关。 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataAccess {
    String resource();
    String permission();
    Operation operation() default Operation.READ;
    View view() default View.CURRENT_SCOPE;

    enum Operation { READ, CREATE, UPDATE, DELETE }
    enum View { CURRENT_SCOPE, AUTHORIZED_SCOPES }
}
