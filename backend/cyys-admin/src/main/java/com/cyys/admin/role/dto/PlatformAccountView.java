package com.cyys.admin.role.dto;

import java.util.List;

public record PlatformAccountView(String id, String loginName, String realName, int status, List<String> roleIds) {}
