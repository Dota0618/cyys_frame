package com.cyys.admin.file.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cyys.admin.file.dto.*;
import com.cyys.admin.file.service.FileService;
import com.cyys.common.web.R;
import com.cyys.common.validation.SafeFileName;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import com.cyys.common.web.DownloadResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/admin/files")
@RequiredArgsConstructor
public class FileController {
    private final FileService service;

    @GetMapping @SaCheckPermission("file:read")
    public R<IPage<FileView>> page(@RequestParam(defaultValue="1") @Min(1) long page,
                                     @RequestParam(defaultValue="20") @Min(1) @Max(100) long size,
                                     @RequestParam(required=false) @Size(max=200) String search,
                                     @RequestParam(required=false) @Size(max=32) String directoryId) {
        return R.ok(service.page(page, size, search, directoryId));
    }

    @PostMapping(consumes="multipart/form-data") @SaCheckPermission("file:upload")
    public R<FileView> upload(@RequestPart("file") @SafeFileName MultipartFile file,
                              @RequestParam(required=false) @Size(max=32) String directoryId) { return R.ok(service.upload(file,directoryId)); }

    @PostMapping("/move") @SaCheckPermission("file:move")
    public R<Void> move(@Valid @RequestBody MoveFilesCommand command) { service.move(command); return R.ok(); }

    @PostMapping("/trash") @SaCheckPermission("file:delete")
    public R<Void> trashBatch(@RequestBody @NotEmpty @Size(max=100) List<@NotNull @Valid FileVersionReference> commands) {
        service.trashBatch(commands); return R.ok();
    }

    @GetMapping("/recycle") @SaCheckPermission("file:recycle:read")
    public R<IPage<FileView>> recycled(@RequestParam(defaultValue="1") @Min(1) long page,
                                      @RequestParam(defaultValue="20") @Min(1) @Max(100) long size,
                                      @RequestParam(required=false) @Size(max=200) String search) {
        return R.ok(service.recycled(page,size,search));
    }

    @PostMapping("/restore") @SaCheckPermission("file:restore")
    public R<Void> restore(@RequestBody @NotEmpty @Size(max=100) List<@NotNull @Valid FileVersionReference> commands) {
        service.restore(commands); return R.ok();
    }

    @DeleteMapping("/recycle/{id}") @SaCheckPermission("file:purge")
    public R<Void> purge(@PathVariable String id,@RequestParam @Min(0) int version) { service.purge(id,version); return R.ok(); }

    @GetMapping("/{id}/content") @SaCheckPermission("file:content:read")
    public void download(@PathVariable String id, HttpServletResponse response) throws IOException {
        try (var file = service.download(id)) {
            DownloadResponse.send(response,file.name(),file.bytes(),file.stream());
        }
    }

    @PutMapping("/{id}") @SaCheckPermission("file:update")
    public R<FileView> rename(@PathVariable String id, @Valid @RequestBody FileRenameCmd command) {
        return R.ok(service.rename(id, command));
    }

    @DeleteMapping("/{id}") @SaCheckPermission("file:delete")
    public R<Void> trash(@PathVariable String id, @RequestParam @Min(0) int version) {
        service.trash(id, version); return R.ok();
    }

    @GetMapping("/uploads/limits") @SaCheckPermission("file:upload")
    public R<FileUploadLimitsDTO> limits() { return R.ok(service.uploadLimits()); }

    @PostMapping("/uploads") @SaCheckPermission("file:upload")
    public R<FileUploadView> start(@Valid @RequestBody FileUploadStartCmd command) { return R.ok(service.startUpload(command)); }

    @GetMapping("/uploads/{id}") @SaCheckPermission("file:upload")
    public R<FileUploadView> progress(@PathVariable String id) { return R.ok(service.uploadProgress(id)); }

    @PutMapping(value="/uploads/{id}/parts/{index}", consumes="multipart/form-data") @SaCheckPermission("file:upload")
    public R<FileUploadView> part(@PathVariable String id, @PathVariable @Min(0) int index, @RequestPart("file") MultipartFile part) {
        return R.ok(service.uploadPart(id, index, part));
    }

    @PostMapping("/uploads/{id}/complete") @SaCheckPermission("file:upload")
    public R<FileView> complete(@PathVariable String id) { return R.ok(service.completeUpload(id)); }

    @PostMapping("/uploads/{id}/cancel") @SaCheckPermission("file:upload")
    public R<Void> cancel(@PathVariable String id) { service.cancelUpload(id); return R.ok(); }

    @PostMapping("/uploads/{id}/cleanup") @SaCheckPermission("file:upload")
    public R<Void> cleanup(@PathVariable String id) { service.cleanupUpload(id); return R.ok(); }
}
