package com.cyys.admin.file.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.file.dto.*;
import com.cyys.admin.file.mapper.FileEntryMapper;
import com.cyys.admin.file.mapper.FileOwnerMapper;
import com.cyys.admin.file.mapper.FilePartMapper;
import com.cyys.admin.file.model.FileEntry;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.storage.ConfiguredStorage;
import com.cyys.common.storage.ObjectStorage;
import com.cyys.common.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.util.List;

@Service
public class FileService {
    private final FileEntryMapper mapper;
    private final FileOwnerMapper owners;
    private final ConfiguredStorage storage;
    private final DataAuditService audit;
    private final FilePartMapper parts;
    private final FileDirectoryService directories;
    private final TransactionTemplate transactions;

    public FileService(FileEntryMapper mapper, FileOwnerMapper owners, ConfiguredStorage storage,
                       DataAuditService audit, FilePartMapper parts, FileDirectoryService directories, PlatformTransactionManager manager) {
        this.mapper = mapper; this.owners = owners; this.storage = storage; this.audit = audit;
        this.parts = parts;
        this.directories = directories;
        this.transactions = new TransactionTemplate(manager);
    }

    @Transactional(readOnly = true)
    public IPage<FileView> page(long page, long size, String search, String directoryId) {
        Page<FileEntry> paging = new Page<>(page, size);
        paging.setOptimizeCountSql(false);
        if(directoryId!=null && !"__root__".equals(directoryId)) directories.requireReadable(directoryId);
        var query=new QueryWrapper<FileEntry>()
                .like(search != null && !search.isBlank(), "name", search)
                .isNull("__root__".equals(directoryId),"dir_id")
                .eq(directoryId!=null && !"__root__".equals(directoryId),"dir_id",directoryId)
                .orderByDesc("created_at", "id");
        return mapper.selectPage(paging,query).convert(FileView::from);
    }

    @Transactional(propagation = Propagation.NEVER)
    public FileView upload(MultipartFile source, String directoryId) {
        String name = safeName(source.getOriginalFilename());
        var target = storage.active();
        if (source.getSize() > target.maxBytes()) throw new ApiException(413, "文件超过允许大小");
        // 先登记对象位置，再写物理文件；崩溃后仍有 UPLOADING 记录供核对，不产生无主路径。
        FileEntry file = transactions.execute(status -> {
            var identity = ScopeContext.get();
            directories.requireTarget(directoryId);
            var entry = new FileEntry();
            entry.setDirId(directoryId);
            entry.setName(name);
            entry.setOrgId(owners.department(identity.loginId(), identity.requireScopeId()));
            entry.setStorageProfile(target.profileId());
            entry.setObjectKey(ConfiguredStorage.newKey());
            entry.setState("UPLOADING");
            requireOne(mapper.insert(entry));
            // 新增也必须在该操作的数据范围内；失败时回滚登记，不开始物理写入。
            if (mapper.selectById(entry.getId()) == null) throw new ApiException(403, "没有当前部门的文件上传权限");
            return entry;
        });
        ObjectStorage.Written written;
        try (InputStream input = source.getInputStream()) {
            written = target.storage().write(file.getObjectKey(), input, target.maxBytes());
        } catch (IOException | RuntimeException error) {
            // 失败对象保留位置和失败状态；清理失败也不会被当成可下载文件。
            try {
                transactions.executeWithoutResult(status -> {
                    FileEntry current = mapper.lock(file.getId());
                    if (current == null) throw new ApiException(409, "上传记录不可访问");
                    current.setState("FAILED");
                    requireOne(mapper.updateById(current));
                });
            } catch (RuntimeException marking) { error.addSuppressed(marking); }
            if (error instanceof ApiException api) throw api;
            throw new ApiException(503, "文件写入失败，请稍后重试");
        }
        // 重新查权；撤权/数据库失败则保留 UPLOADING，绝不返回假成功。
        return transactions.execute(status -> {
            FileEntry current = mapper.lock(file.getId());
            if (current == null || !"UPLOADING".equals(current.getState())) throw new ApiException(409, "上传记录状态已变化");
            current.setByteSize(written.bytes());
            current.setSha256(written.sha256());
            current.setState("READY");
            requireOne(mapper.updateById(current));
            audit.success("file", current.getId(), "UPLOAD");
            return FileView.from(current);
        });
    }

    /** 同步打开流，授权完成后才读取；Controller 在当前请求内负责关闭。 */
    public Download download(String id) {
        FileEntry file = mapper.selectById(id);
        if (file == null) throw new ApiException(404, "文件不存在或不可访问");
        if (!"READY".equals(file.getState())) throw new ApiException(409, "文件尚不可下载");
        try {
            return new Download(file.getName(), file.getByteSize(), storage.resolve(file.getStorageProfile()).storage().read(file.getObjectKey()));
        } catch (IOException error) { throw new ApiException(503, "文件内容暂不可用"); }
    }

    @Transactional
    public FileView rename(String id, FileRenameCmd command) {
        FileEntry file = mapper.lock(id);
        if (file == null || !file.getVersion().equals(command.version())) throw new ApiException(409, "文件不可修改或版本已变化");
        file.setName(safeName(command.name()));
        requireOne(mapper.updateById(file));
        audit.success("file", id, "UPDATE");
        return FileView.from(file);
    }

    @Transactional
    public void trash(String id, int version) {
        FileEntry file = mapper.lock(id);
        if (file == null || file.getVersion() != version || "UPLOADING".equals(file.getState())) {
            throw new ApiException(409, "文件不可删除、正在上传或版本已变化");
        }
        if (file.getPartBytes() != null || "FAILED".equals(file.getState())) {
            var target = storage.resolve(file.getStorageProfile());
            try {
                for (var part : parts.list(id, file.getScopeId(), file.getCreatedBy())) target.storage().delete(part.objectKey());
                if ("FAILED".equals(file.getState())) target.storage().delete(file.getObjectKey());
            } catch (IOException error) { throw new ApiException(503, "临时文件清理失败，尚未删除记录"); }
        }
        requireOne(mapper.trash(id, version, ScopeContext.get().loginId()));
        audit.success("file", id, "DELETE");
    }

    @Transactional
    public void trashBatch(List<FileVersionReference> commands) {
        validateDistinct(commands);
        // 全部授权和版本先复核；任何一项失败都不开始清理分片或改变记录。
        for(var command:commands.stream().sorted(java.util.Comparator.comparing(FileVersionReference::id)).toList()) {
            FileEntry file=mapper.lock(command.id());
            if(file==null || !file.getVersion().equals(command.version()) || "UPLOADING".equals(file.getState())) {
                throw new ApiException(409,"批次包含不可删除、正在上传或版本已变化的文件");
            }
        }
        for(var command:commands) trash(command.id(),command.version());
    }

    @Transactional
    public void move(MoveFilesCommand command) {
        validateDistinct(command.files());
        directories.requireTarget(command.directoryId());
        for(var reference:command.files().stream().sorted(java.util.Comparator.comparing(FileVersionReference::id)).toList()) {
            var file=mapper.lock(reference.id());
            if(file==null || !file.getVersion().equals(reference.version())
                    || !file.getCreatedBy().equals(ScopeContext.get().loginId())) throw new ApiException(409,"批次包含不可移动或版本已变化的文件");
        }
        for(var reference:command.files()) {
            requireOne(mapper.move(reference.id(),reference.version(),command.directoryId(),ScopeContext.get().loginId()));
            audit.success("file",reference.id(),"MOVE");
        }
    }

    static void validateDistinct(List<FileVersionReference> commands) {
        if(commands.stream().map(FileVersionReference::id).distinct().count()!=commands.size()) throw new ApiException(400,"批次不能包含重复文件");
    }

    static String safeName(String value) {
        if (value == null || value.isBlank() || value.length() > 200 || value.equals(".") || value.equals("..")
                || value.indexOf('/') >= 0 || value.indexOf('\\') >= 0 || value.codePoints().anyMatch(Character::isISOControl)) {
            throw new ApiException(400, "文件名不能为空、超过200字或包含路径和控制字符");
        }
        return value.strip();
    }

    private static void requireOne(int count) {
        if (count != 1) throw new ApiException(409, "文件不可操作、正在上传或版本已变化");
    }
    public record Download(String name, long bytes, InputStream stream) implements AutoCloseable {
        @Override public void close() throws IOException { stream.close(); }
    }
}
