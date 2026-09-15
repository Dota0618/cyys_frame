package com.cyys.admin.user.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

public final class MemberDTO {
    private MemberDTO() {}
    public record Query(@Min(1) long page, @Min(1) @Max(200) long size, @Size(max=100) String name) {}
    public record Create(@NotBlank @Pattern(regexp="[A-Za-z0-9][A-Za-z0-9_.-]{2,63}") String loginName,
                         @NotBlank @Size(min=12,max=72) String password,
                         @NotBlank @Size(max=100) String displayName, @Size(max=32) String orgId) {}
    public record Edit(@NotNull @Min(0) Integer version, @NotBlank @Size(max=100) String displayName,
                       @Size(max=32) String orgId, @NotNull @Min(0) @Max(1) Integer status) {}
    public record View(String id, String userId, String scopeId, String loginName, String displayName,
                       String orgId, int status, int version, LocalDateTime createdAt) {}
    public record Detail(View member, List<String> roleIds) {}
    public record Option(String value, String label) {}
    public record DepartmentOption(String value, String label, String parentId) {}
}
