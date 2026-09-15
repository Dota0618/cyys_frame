package com.cyys.admin.role.dto;

import com.cyys.admin.role.model.SysRole;
import jakarta.validation.constraints.*;
import java.util.List;

public final class RoleDTO {
    private RoleDTO() {}

    public record Query(@Min(1) long page, @Min(1) @Max(200) long size,
                        @Size(max = 100) String search) {}

    public record Create(@NotBlank @Size(max = 64) String code,
                         @NotBlank @Size(max = 50) String name,
                         @NotNull @Min(1) @Max(5) Integer dataRange,
                         @NotNull @Min(0) @Max(1) Integer status,
                         @NotNull Integer sort,
                         @Size(max = 500) String remark,
                         @NotNull @Size(max = 500) List<@NotBlank @Size(max = 32) String> menuIds,
                         @NotNull @Size(max = 500) List<@NotBlank @Size(max = 32) String> orgIds) {}

    public record Edit(@NotBlank @Size(max = 32) String id,
                       @NotNull @Min(0) Integer version,
                       @NotBlank @Size(max = 50) String name,
                       @NotNull @Min(1) @Max(5) Integer dataRange,
                       @NotNull @Min(0) @Max(1) Integer status,
                       @NotNull Integer sort,
                       @Size(max = 500) String remark,
                       @NotNull @Size(max = 500) List<@NotBlank @Size(max = 32) String> menuIds,
                       @NotNull @Size(max = 500) List<@NotBlank @Size(max = 32) String> orgIds) {}

    public record View(String id, String scopeId, String code, String name, int dataRange,
                       int status, int sort, int version, String remark) {
        public static View from(SysRole role) {
            return new View(role.getId(), role.getScopeId(), role.getCode(), role.getName(),
                    role.getDataRange(), role.getStatus(), role.getSort(), role.getVersion(), role.getRemark());
        }
    }

    public record MenuOption(String id, String parentId, String name, String code, int type) {}
    public record DepartmentOption(String id, String parentId, String name) {}
    public record Options(List<MenuOption> menus, List<DepartmentOption> departments) {}
    public record Detail(View role, List<String> menuIds, List<String> orgIds, boolean editable) {}
}
