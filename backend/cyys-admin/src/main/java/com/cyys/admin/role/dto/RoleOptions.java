package com.cyys.admin.role.dto;

import java.util.List;

public record RoleOptions(List<RoleMenuOption> menus, List<RoleDepartmentOption> departments) {}
