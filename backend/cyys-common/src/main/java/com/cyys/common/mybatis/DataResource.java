package com.cyys.common.mybatis;

public record DataResource(String name, String table, String scopeColumn, String departmentColumn,
                           String ancestorsColumn, String actorColumn, String versionColumn, java.util.Set<String> immutableColumns) {
    public DataResource {
        for (String identifier : new String[]{name, table, scopeColumn, departmentColumn, actorColumn}) {
            if (identifier == null || !identifier.matches("[a-z][a-z0-9_]*")) {
                throw new IllegalArgumentException("资源及字段必须使用固定的服务端标识");
            }
        }
        if (ancestorsColumn != null && !ancestorsColumn.matches("[a-z][a-z0-9_]*")) {
            throw new IllegalArgumentException("非法祖先字段");
        }
        if (versionColumn != null && !versionColumn.matches("[a-z][a-z0-9_]*")) {
            throw new IllegalArgumentException("非法版本字段");
        }
        immutableColumns = java.util.Set.copyOf(immutableColumns);
    }
}
