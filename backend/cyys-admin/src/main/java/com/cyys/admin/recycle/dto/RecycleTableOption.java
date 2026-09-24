package com.cyys.admin.recycle.dto;

import java.util.List;

public record RecycleTableOption(String tableName, List<RecycleColumnOption> columns) {}
