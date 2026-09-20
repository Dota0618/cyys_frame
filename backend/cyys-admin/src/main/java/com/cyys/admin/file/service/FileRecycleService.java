package com.cyys.admin.file.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.file.dto.FileView;
import com.cyys.admin.file.dto.FileVersionReference;
import com.cyys.admin.file.mapper.FileEntryMapper;
import com.cyys.admin.file.mapper.FilePartMapper;
import com.cyys.admin.file.model.FileEntry;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.storage.ConfiguredStorage;
import com.cyys.common.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.io.IOException;
import java.util.List;

@Service
public class FileRecycleService {
    private final FileEntryMapper files;
    private final FilePartMapper parts;
    private final ConfiguredStorage storage;
    private final DataAuditService audit;
    private final TransactionTemplate transactions;

    public FileRecycleService(FileEntryMapper files,FilePartMapper parts,ConfiguredStorage storage,
                              DataAuditService audit,PlatformTransactionManager manager) {
        this.files=files; this.parts=parts; this.storage=storage; this.audit=audit;
        this.transactions=new TransactionTemplate(manager);
    }

    @Transactional(readOnly=true)
    public IPage<FileView> page(long page,long size,String search) {
        Page<FileEntry> query=new Page<>(page,size); query.setOptimizeCountSql(false);
        return files.recycle(query,search).convert(FileView::from);
    }

    @Transactional
    public void restore(List<FileVersionReference> commands) {
        FileService.validateDistinct(commands);
        for(var command:commands.stream().sorted(java.util.Comparator.comparing(FileVersionReference::id)).toList()) {
            var file=files.lockRecycled(command.id());
            if(file==null || !file.getVersion().equals(command.version()) || !"READY".equals(file.getState())) {
                throw new ApiException(409,"批次包含不可恢复、待清理或版本已变化的文件");
            }
        }
        for(var command:commands) {
            requireOne(files.restore(command.id(),command.version(),ScopeContext.get().loginId()));
            audit.success("file",command.id(),"RESTORE");
        }
    }

    /** 先持久化不可恢复状态，再删除登记对象；失败可重试，不能把物理删除当成 DB 事务。 */
    @Transactional(propagation=Propagation.NEVER)
    public void purge(String id,int version) {
        var file=transactions.execute(status -> {
            var current=files.lockRecycled(id);
            if(current==null || current.getVersion()!=version || !List.of("READY","FAILED","PURGING").contains(current.getState())) {
                throw new ApiException(409,"文件不可永久删除或版本已变化");
            }
            if(!"PURGING".equals(current.getState())) {
                requireOne(files.purgeState(id,version,"PURGING",ScopeContext.get().loginId()));
                current.setVersion(version+1); current.setState("PURGING");
            }
            return current;
        });
        try {
            var target=storage.resolve(file.getStorageProfile());
            for(var part:parts.list(id,file.getScopeId(),file.getCreatedBy())) target.storage().delete(part.objectKey());
            target.storage().delete(file.getObjectKey());
        } catch(IOException error) { throw new ApiException(503,"物理文件清理失败，记录保留待清理状态，请刷新后重试"); }
        transactions.executeWithoutResult(status -> {
            var current=files.lockRecycled(id);
            if(current==null || !"PURGING".equals(current.getState())) throw new ApiException(409,"文件状态已变化，请刷新");
            requireOne(files.purgeState(id,current.getVersion(),"PURGED",ScopeContext.get().loginId()));
            audit.success("file",id,"PURGE");
        });
    }
    private static void requireOne(int count) { if(count!=1) throw new ApiException(409,"文件状态或授权已变化，请刷新"); }
}
