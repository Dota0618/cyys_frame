package com.cyys.admin.file.service;

import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.file.dto.*;
import com.cyys.admin.file.mapper.*;
import com.cyys.admin.file.model.FileEntry;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.storage.*;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.time.LocalDateTime;
import java.util.List;

/** 元数据和分片均持久化；每次重试按当前权限查库，不依赖内存会话。 */
@Service
@RequiredArgsConstructor
public class ResumableUploadService {
    private final FileEntryMapper files;
    private final FilePartMapper parts;
    private final FileOwnerMapper owners;
    private final ConfiguredStorage storage;
    private final StorageProperties settings;
    private final DataAuditService audit;
    private final FileDirectoryService directories;
    private final TemporaryFileCleaner temporaryCleaner;

    public FileUploadLimitsDTO limits() {
        if (settings.getPartBytes() < 1024 || settings.getPartBytes() > settings.getMaxBytes()
                || settings.getMaxResumableBytes() < settings.getMaxBytes()) throw new ApiException(503, "上传大小配置无效");
        return new FileUploadLimitsDTO(settings.getMaxBytes(), settings.getMaxResumableBytes(), settings.getPartBytes());
    }

    @Transactional
    public FileUploadView start(FileUploadStartCmd command) {
        var limits = limits();
        if (command.bytes() > limits.resumableBytes()) throw new ApiException(413, "文件超过允许大小");
        long count = (command.bytes() - 1) / limits.partBytes() + 1;
        if (count != command.hashes().size()) throw new ApiException(400, "分片清单与文件大小不匹配");
        var target = storage.active();
        var identity = ScopeContext.get();
        directories.requireTarget(command.directoryId());
        FileEntry file = new FileEntry();
        file.setDirId(command.directoryId());
        file.setName(command.name().strip());
        file.setOrgId(owners.department(identity.loginId(), identity.requireScopeId()));
        file.setStorageProfile(target.profileId()); file.setObjectKey(ConfiguredStorage.newKey());
        file.setState("UPLOADING"); file.setExpectedBytes(command.bytes()); file.setPartBytes(limits.partBytes());
        file.setExpiresAt(LocalDateTime.now().plusDays(7));
        requireOne(files.insert(file));
        if (files.selectById(file.getId()) == null) throw new ApiException(403, "没有当前部门的文件上传权限");
        for (int index = 0; index < command.hashes().size(); index++) {
            requireOne(parts.insert(file.getId(), file.getScopeId(), file.getCreatedBy(),
                    new FilePartMapper.Part(index, ConfiguredStorage.newKey(), command.hashes().get(index), false)));
        }
        return view(file);
    }

    @Transactional(readOnly = true)
    public FileUploadView progress(String id) { return view(owned(id, false)); }

    @Transactional
    public FileUploadView put(String id, int index, MultipartFile source) {
        FileEntry file = owned(id, true);
        uploading(file);
        List<FilePartMapper.Part> entries = entries(file);
        if (index < 0 || index >= entries.size()) throw new ApiException(400, "分片编号无效");
        var part = entries.get(index);
        long expected = Math.min(file.getPartBytes(), file.getExpectedBytes() - (long) index * file.getPartBytes());
        if (source.getSize() != expected) throw new ApiException(400, "分片大小不匹配");
        if (part.ready()) return view(file);
        var target = storage.resolve(file.getStorageProfile());
        try (var input = source.getInputStream()) {
            // 行锁串行化上传/完成/取消；仅清理已登记且尚未确认的对象。
            target.storage().delete(part.objectKey());
            var written = target.storage().write(part.objectKey(), input, expected);
            if (written.bytes() != expected || !written.sha256().equals(part.expectedSha256())) {
                target.storage().delete(part.objectKey());
                throw new ApiException(422, "分片校验失败，请重新选择原文件");
            }
            requireOne(parts.ready(id, file.getScopeId(), file.getCreatedBy(), index));
            return view(file);
        } catch (IOException error) { throw new ApiException(503, "分片写入失败，可以重试"); }
    }

    @Transactional(noRollbackFor = FilePartRepairRequiredException.class)
    public FileView complete(String id) {
        FileEntry file = owned(id, true);
        if ("READY".equals(file.getState())) return FileView.from(file);
        uploading(file);
        var entries = entries(file);
        if (entries.isEmpty() || entries.stream().anyMatch(part -> !part.ready())) throw new ApiException(409, "仍有分片未上传");
        var target = storage.resolve(file.getStorageProfile());
        try {
            target.storage().delete(file.getObjectKey());
            try (InputStream input = new VerifiedPartsInputStream(target.storage(), entries, file.getExpectedBytes(), file.getPartBytes())) {
                var written = target.storage().write(file.getObjectKey(), input, file.getExpectedBytes());
                if (written.bytes() != file.getExpectedBytes()) throw new ApiException(422, "文件大小校验失败");
                file.setByteSize(written.bytes()); file.setSha256(written.sha256());
            }
        } catch (CorruptFilePartException corrupt) {
            requireOne(parts.pending(id, file.getScopeId(), file.getCreatedBy(), corrupt.index));
            throw new FilePartRepairRequiredException();
        } catch (IOException error) { throw new ApiException(503, "文件合并失败，可以重试"); }
        file.setState("READY");
        requireOne(files.updateById(file));
        audit.success("file", id, "UPLOAD");
        return FileView.from(file);
    }

    /** 完成后可重复清理分片；失败保留元数据和原文件，供重试。 */
    @Transactional
    public void cleanup(String id) {
        FileEntry file = owned(id, true);
        if ("UPLOADING".equals(file.getState())) throw new ApiException(409, "请先完成或取消上传");
        temporaryCleaner.clean(file);
        requireOne(files.temporaryCleaned(id,file.getVersion()));
    }

    @Transactional
    public void cancel(String id) {
        FileEntry file = owned(id, true);
        if ("READY".equals(file.getState())) throw new ApiException(409, "文件已完成上传");
        if ("FAILED".equals(file.getState())) return;
        file.setState("FAILED"); requireOne(files.updateById(file));
        audit.success("file", id, "CANCEL_UPLOAD");
    }

    private FileEntry owned(String id, boolean lock) {
        FileEntry file = lock ? files.lock(id) : files.selectById(id);
        if (file == null || file.getPartBytes() == null || !file.getCreatedBy().equals(ScopeContext.get().loginId())) {
            throw new ApiException(404, "上传任务不存在或不可访问");
        }
        return file;
    }
    private void uploading(FileEntry file) {
        if (!"UPLOADING".equals(file.getState())) throw new ApiException(409, "上传任务不可继续");
        if (!file.getExpiresAt().isAfter(LocalDateTime.now())) throw new ApiException(410, "上传任务已过期，请取消后重新上传");
    }
    private List<FilePartMapper.Part> entries(FileEntry file) {
        return parts.list(file.getId(), file.getScopeId(), file.getCreatedBy());
    }
    private FileUploadView view(FileEntry file) {
        var entries = entries(file);
        return new FileUploadView(file.getId(), file.getName(), file.getExpectedBytes(), file.getPartBytes(), file.getState(),
                file.getExpiresAt(), entries.stream().filter(FilePartMapper.Part::ready).map(FilePartMapper.Part::partIndex).toList(),
                entries.stream().map(FilePartMapper.Part::expectedSha256).toList());
    }
    private static void requireOne(int count) { if (count != 1) throw new ApiException(409, "上传任务已变化，请刷新后重试"); }
}
