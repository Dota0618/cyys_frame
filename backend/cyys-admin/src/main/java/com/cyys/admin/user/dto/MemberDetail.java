package com.cyys.admin.user.dto;

import java.util.List;

public record MemberDetail(MemberView member, List<String> roleIds) {}
