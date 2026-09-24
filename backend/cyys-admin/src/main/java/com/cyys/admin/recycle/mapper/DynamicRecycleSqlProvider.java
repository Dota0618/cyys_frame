package com.cyys.admin.recycle.mapper;

import com.cyys.admin.recycle.model.RecycleConfiguration;
import java.util.Map;

/** 仅拼接服务端验证过的标识符；值和数据范围仍全部使用 MyBatis 参数。 */
public final class DynamicRecycleSqlProvider {
    private static final String IDENTIFIER = "[a-z][a-z0-9_]{0,63}";

    private DynamicRecycleSqlProvider() {}

    public static String page(Map<String, Object> params) {
        var d = definition(params);
        String scope = d.getScopeRequired() == 1 ? "r." + id(d.getScopeColumn()) : "null";
        String department = d.getScopeRequired() == 1 && d.getDepartmentColumn() != null
                ? "r." + id(d.getDepartmentColumn()) : "null";
        String actor = d.getUpdatedByColumn() == null || d.getUpdatedByColumn().isBlank()
                ? "null" : "r." + id(d.getUpdatedByColumn());
        String sql = "select r." + id("id") + " id,r." + id(d.getTitleColumn()) + " title," + scope
                + " scope_id,null scope_name," + department + " department_id,null department_name," + actor
                + " deleted_by,null deleted_by_name,r." + id(d.getUpdatedAtColumn()) + " deleted_at,r."
                + id(d.getCreatedAtColumn())
                + " created_at,r." + id(d.getVersionColumn()) + " version from " + id(d.getTableName())
                + " r where r." + id(d.getDeletedColumn()) + "=1 and (" + predicate(params) + ")";
        if (params.get("search") != null && !params.get("search").toString().isBlank()) {
            sql += " and r." + id(d.getSearchColumn()) + " like concat('%',#{search},'%')";
        }
        return sql + " order by r." + id(d.getCreatedAtColumn()) + " desc,r.id desc";
    }

    public static String lock(Map<String, Object> params) {
        var d = definition(params);
        return "select r.id id,r." + id(d.getVersionColumn()) + " version from " + id(d.getTableName())
                + " r where r.id=#{id} and r." + id(d.getDeletedColumn()) + "=1 and (" + predicate(params) + ") for update";
    }

    public static String restore(Map<String, Object> params) {
        var d = definition(params);
        return "update " + id(d.getTableName()) + " set " + id(d.getDeletedColumn()) + "=0,"
                + id(d.getVersionColumn()) + "=" + id(d.getVersionColumn()) + "+1,"
                + id(d.getUpdatedByColumn()) + "=#{actor}," + id(d.getUpdatedAtColumn())
                + "=CURRENT_TIMESTAMP where id=#{id} and " + id(d.getVersionColumn()) + "=#{version} and "
                + id(d.getDeletedColumn()) + "=1 and (" + predicate(params) + ")";
    }

    public static String purge(Map<String, Object> params) {
        var d = definition(params);
        return "delete from " + id(d.getTableName()) + " where id=#{id} and " + id(d.getVersionColumn())
                + "=#{version} and " + id(d.getDeletedColumn()) + "=1 and (" + predicate(params) + ")";
    }

    private static RecycleConfiguration definition(Map<String, Object> params) {
        return (RecycleConfiguration) params.get("definition");
    }

    private static String predicate(Map<String, Object> params) {
        Object value = params.get("predicate");
        if (value == null || value.toString().isBlank()) throw new IllegalArgumentException("回收站数据范围条件缺失");
        return value.toString();
    }

    static String id(String value) {
        if (value == null || !value.matches(IDENTIFIER)) throw new IllegalArgumentException("回收站字段不是受控标识");
        return "`" + value + "`";
    }
}
