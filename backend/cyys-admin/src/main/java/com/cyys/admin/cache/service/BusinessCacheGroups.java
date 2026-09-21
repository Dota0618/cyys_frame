package com.cyys.admin.cache.service;

import com.cyys.common.cache.*;
import java.util.List;

/** 服务端业务组目录；管理页面和数据写入引用同一个定义。 */
public final class BusinessCacheGroups {
    public static final CacheRegion USER_NAMES = new CacheRegion("user-display", CacheScope.UNIT);
    public static final CacheRegion DICTIONARY_OPTIONS = new CacheRegion("dictionary-options", CacheScope.SYSTEM);
    public static final CacheRegion DICTIONARY_LABELS = new CacheRegion("dictionary-labels", CacheScope.SYSTEM);
    public static final CacheGroup USERS = new CacheGroup("user-display", List.of(USER_NAMES));
    public static final CacheGroup DICTIONARIES = new CacheGroup("dictionary", List.of(DICTIONARY_OPTIONS, DICTIONARY_LABELS));
    private BusinessCacheGroups() {}
}
