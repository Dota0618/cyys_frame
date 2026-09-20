package com.cyys.admin.user.dto;

import jakarta.validation.constraints.*;

public record MemberQuery(@Min(1) long page, @Min(1) @Max(200) long size, @Size(max=100) String name) {}
