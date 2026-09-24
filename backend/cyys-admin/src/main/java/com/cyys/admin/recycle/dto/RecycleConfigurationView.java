package com.cyys.admin.recycle.dto;

public record RecycleConfigurationView(
        String id, String code, String name, String tableName,
        String titleColumn, String searchColumn, boolean scopeRequired,
        String scopeColumn, String departmentColumn, String ancestorsColumn,
        String actorColumn, String versionColumn, String deletedColumn,
        String createdAtColumn, String updatedAtColumn, String updatedByColumn,
        int sort, int version, boolean enabled,
        String readPermission, String restorePermission, String purgePermission) {}
