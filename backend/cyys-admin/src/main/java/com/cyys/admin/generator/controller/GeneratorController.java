package com.cyys.admin.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyys.admin.generator.dto.GenerateCodeCommand;
import com.cyys.admin.generator.service.GeneratorService;
import com.cyys.common.web.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@RestController
@Validated
@PlatformControl
@RequiredArgsConstructor
@RequestMapping("/api/admin/platform/generator")
public class GeneratorController {
    private final GeneratorService service;

    @GetMapping("/tables") @SaCheckPermission("generator:read")
    public Object tables() { return R.ok(service.tables()); }

    @GetMapping("/tables/{table}") @SaCheckPermission("generator:read")
    public Object columns(@PathVariable @Pattern(regexp="biz_[a-z][a-z0-9_]{0,43}") String table) { return R.ok(service.columns(table)); }

    @PostMapping("/preview") @SaCheckPermission("generator:read")
    public Object preview(@Valid @RequestBody GenerateCodeCommand command) { return R.ok(service.preview(command)); }

    @PostMapping("/download") @SaCheckPermission("generator:generate")
    public void download(@Valid @RequestBody GenerateCodeCommand command, HttpServletResponse response) throws IOException {
        byte[] bytes=service.download(command);
        try(var input=new ByteArrayInputStream(bytes)) { DownloadResponse.send(response,command.feature()+"-generated.zip",bytes.length,input); }
    }
}
