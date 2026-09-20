package com.cyys.admin.role.dto;

import java.util.List;

public record PlatformRoleDetail(PlatformRoleView role, List<String> menuIds, boolean editable) {}
