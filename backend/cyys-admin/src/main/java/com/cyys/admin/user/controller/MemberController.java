package com.cyys.admin.user.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyys.admin.user.dto.MemberDTO;
import com.cyys.admin.user.service.MemberService;
import com.cyys.common.web.AuthorizedScopesRead;
import com.cyys.common.web.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class MemberController {
    private final MemberService service;
    @GetMapping @SaCheckPermission("user:read")
    public Object page(@RequestParam(defaultValue="1") long page,@RequestParam(defaultValue="20") long size,@RequestParam(required=false) String name) {
        return R.ok(service.page(new MemberDTO.Query(page,size,name)));
    }
    @GetMapping("/all") @AuthorizedScopesRead @SaCheckPermission("user:read")
    public Object all(@RequestParam(defaultValue="1") long page,@RequestParam(defaultValue="20") long size) {
        return R.ok(service.all(new MemberDTO.Query(page,size,null)));
    }
    @GetMapping("/{id}") @SaCheckPermission("user:read")
    public Object detail(@PathVariable String id) {return R.ok(service.detail(id));}
    @PostMapping @SaCheckPermission("user:create")
    public Object create(@Valid @RequestBody MemberDTO.Create command) {return R.ok(service.create(command));}
    @PutMapping("/{id}") @SaCheckPermission("user:update")
    public Object edit(@PathVariable String id,@Valid @RequestBody MemberDTO.Edit command) {return R.ok(service.edit(id,command));}
    @PutMapping("/{id}/roles") @SaCheckPermission("role:grant")
    public Object grant(@PathVariable String id,@RequestBody List<String> roles) {service.grantRoles(id,roles);return R.ok();}
    @GetMapping("/departments") @SaCheckPermission("user:read")
    public Object departments() {return R.ok(service.departments());}
    @GetMapping("/roles") @SaCheckPermission("role:grant")
    public Object roles() {return R.ok(service.roles());}
}
