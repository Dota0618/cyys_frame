package com.cyys.admin.user.dto;

import java.util.List;

public record MemberDataPermissionView(String memberId, int version, int dataRange, List<String> orgIds,
                                       List<MemberDepartmentOption> departments) {}
