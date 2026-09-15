package com.cyys.admin.department.dto;

import com.cyys.admin.department.model.SysOrg;
import jakarta.validation.constraints.*;

public final class DepartmentDTO {
    private DepartmentDTO() {}

    public record Create(@NotBlank @Size(max = 100) String name, @Size(max = 64) String code,
                         @Size(max = 32) String parentId) {}
    public record Edit(@NotBlank @Size(max = 32) String id, @NotNull @Min(0) Integer version,
                       @NotBlank @Size(max = 100) String name) {}
    public record VersionedId(@NotBlank @Size(max = 32) String id, @NotNull @Min(0) Integer version) {}
    public record Query(@Min(1) long page, @Min(1) @Max(200) long size,
                        @Size(max = 100) String search) {}
    public record Option(String value, String label, String parentId) {}
    public record View(String id, String scopeId, String parentId, String name, String code, int version) {
        public static View from(SysOrg org) {
            return new View(org.getId(), org.getScopeId(), org.getParentId(), org.getName(), org.getCode(), org.getVersion());
        }
    }
}
