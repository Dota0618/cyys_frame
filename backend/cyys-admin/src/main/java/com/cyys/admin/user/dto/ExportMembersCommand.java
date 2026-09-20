package com.cyys.admin.user.dto;

import jakarta.validation.constraints.Size;

public record ExportMembersCommand(@Size(max = 100) String name) {}
