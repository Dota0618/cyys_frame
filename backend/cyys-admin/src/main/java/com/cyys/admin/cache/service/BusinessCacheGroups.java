package com.cyys.admin.cache.service;

import com.cyys.common.cache.*;
import java.util.List;

/** 服务端业务组目录；管理页面和数据写入引用同一个定义。 */
public final class BusinessCacheGroups {
    // 一个账号只有一份名称；跨单位引用使用同一最小记录，改名按账号 ID 失效。
    public static final CacheRegion USER_NAMES = new CacheRegion("user-records", CacheScope.SYSTEM);
    public static final CacheRegion SCOPE_NAMES = new CacheRegion("scope-records", CacheScope.SYSTEM);
    public static final CacheRegion DEPARTMENT_NAMES = new CacheRegion("department-records", CacheScope.UNIT);
    public static final CacheRegion DICTIONARY_OPTIONS = new CacheRegion("dictionary-options", CacheScope.SYSTEM);
    public static final CacheRegion DICTIONARY_LABELS = new CacheRegion("dictionary-labels", CacheScope.SYSTEM);
    public static final CacheRegion CONFIGURATION_VALUES = new CacheRegion("configuration-values", CacheScope.SYSTEM);
    public static final CacheGroup USERS = new CacheGroup("user-display", List.of(USER_NAMES));
    public static final CacheGroup SCOPES = new CacheGroup("scope", List.of(SCOPE_NAMES));
    public static final CacheGroup DEPARTMENTS = new CacheGroup("department", List.of(DEPARTMENT_NAMES));
    public static final CacheGroup DICTIONARIES = new CacheGroup("dictionary", List.of(DICTIONARY_OPTIONS, DICTIONARY_LABELS));
    public static final CacheGroup CONFIGURATIONS = new CacheGroup("configuration", List.of(CONFIGURATION_VALUES));
    private BusinessCacheGroups() {}
}
