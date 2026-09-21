package com.cyys.admin.user.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyys.admin.user.dto.MemberQuery;
import com.cyys.admin.user.dto.ExportMembersCommand;
import com.cyys.admin.user.dto.CreateMemberCommand;
import com.cyys.admin.user.dto.UpdateMemberCommand;
import com.cyys.admin.user.service.MemberService;
import com.cyys.common.web.AuthorizedScopesRead;
import com.cyys.common.web.R;
import com.cyys.common.web.DownloadResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class MemberController {
    private final MemberService service;
    private final com.cyys.admin.user.service.MemberDataPermissionService dataPermissions;
    @GetMapping("/{id}/data-permission") @SaCheckPermission("user:data-permission")
    public Object dataPermission(@PathVariable String id) { return R.ok(dataPermissions.detail(id)); }
    @PutMapping("/{id}/data-permission") @SaCheckPermission("user:data-permission")
    public Object updateDataPermission(@PathVariable String id,
            @Valid @RequestBody com.cyys.admin.user.dto.UpdateMemberDataPermissionCommand command) {
        return R.ok(dataPermissions.update(id, command));
    }
    @PostMapping("/export") @SaCheckPermission("user:export")
    public void export(@Valid @RequestBody ExportMembersCommand command, HttpServletResponse response) throws IOException {
        byte[] content = service.export(command);
        try (var input = new ByteArrayInputStream(content)) {
            DownloadResponse.send(response, "单位成员.xlsx", content.length, input);
        }
    }
    @GetMapping @SaCheckPermission("user:read")
    public Object page(@RequestParam(defaultValue="1") long page,@RequestParam(defaultValue="20") long size,@RequestParam(required=false) String name) {
        return R.ok(service.page(new MemberQuery(page,size,name)));
    }
    @GetMapping("/all") @AuthorizedScopesRead @SaCheckPermission("user:read")
    public Object all(@RequestParam(defaultValue="1") long page,@RequestParam(defaultValue="20") long size) {
        return R.ok(service.all(new MemberQuery(page,size,null)));
    }
    @GetMapping("/{id}") @SaCheckPermission("user:read")
    public Object detail(@PathVariable String id) {return R.ok(service.detail(id));}
    @PostMapping @SaCheckPermission("user:create")
    public Object create(@Valid @RequestBody CreateMemberCommand command) {return R.ok(service.create(command));}
    @PutMapping("/{id}") @SaCheckPermission("user:update")
    public Object edit(@PathVariable String id,@Valid @RequestBody UpdateMemberCommand command) {return R.ok(service.edit(id,command));}
    @PutMapping("/{id}/roles") @SaCheckPermission("role:grant")
    public Object grant(@PathVariable String id,@RequestBody List<String> roles) {service.grantRoles(id,roles);return R.ok();}
    @GetMapping("/departments") @SaCheckPermission("user:read")
    public Object departments() {return R.ok(service.departments());}
    @GetMapping("/roles") @SaCheckPermission("role:grant")
    public Object roles() {return R.ok(service.roles());}
}
