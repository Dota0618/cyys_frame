package com.cyys.common.cache;

import java.util.List;
import java.util.Map;

/** 来源由代码登记；返回类型必须是只含可展示字段的独立 record。 */
public interface CacheValueSource<T extends Record> {
    String name();
    CacheScope scope();
    Class<T> recordType();
    default CacheShape shape() { return CacheShape.RECORD; }
    default String listKey() { return "id"; }
    default Map<String, T> records(String scopeId, String variant, List<String> ids) { return Map.of(); }
    default List<T> list(String scopeId, String variant) { return List.of(); }
}
