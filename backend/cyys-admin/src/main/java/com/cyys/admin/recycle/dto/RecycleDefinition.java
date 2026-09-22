package com.cyys.admin.recycle.dto;

public record RecycleDefinition(String code, String name, String searchLabel, String description,
                                boolean scopeRequired, String readPermission,
                                String restorePermission, String purgePermission) {}
