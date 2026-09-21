package com.cyys.admin.role.dto;

import java.util.List;

public record RoleDetail(RoleView role, List<String> menuIds, boolean editable) {}
