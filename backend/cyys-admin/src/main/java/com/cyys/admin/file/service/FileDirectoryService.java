package com.cyys.admin.file.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.file.dto.*;
import com.cyys.admin.file.mapper.*;
import com.cyys.admin.file.model.FileDirectory;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/** 沿用个人扁平分类；目录不对应磁盘路径，也不扩张文件授权。 */
@Service
@RequiredArgsConstructor
public class FileDirectoryService {
    private final FileDirectoryMapper directories;
    private final FileEntryMapper files;
    private final FileOwnerMapper owners;
    private final FileDirectoryReferenceMapper references;
    private final DataAuditService audit;

    @Transactional(readOnly=true)
    public List<FileDirectoryView> list() {
        return directories.selectList(new QueryWrapper<FileDirectory>().eq("created_by", ScopeContext.get().loginId())
                .orderByDesc("created_at", "id")).stream().map(FileDirectoryView::from).toList();
    }

    @Transactional
    public FileDirectoryView create(CreateFileDirectoryCommand command) {
        var identity=ScopeContext.get();
        var directory=new FileDirectory();
        directory.setName(command.name().strip());
        directory.setOrgId(owners.department(identity.loginId(),identity.requireScopeId()));
        requireOne(directories.insert(directory));
        if(directories.selectById(directory.getId())==null) throw new ApiException(403,"没有当前部门的目录创建权限");
        audit.success("file-directory",directory.getId(),"CREATE");
        return FileDirectoryView.from(directory);
    }

    @Transactional
    public FileDirectoryView rename(String id, UpdateFileDirectoryCommand command) {
        var directory=lock(id);
        if(!directory.getVersion().equals(command.version())) throw new ApiException(409,"目录版本已变化");
        directory.setName(command.name().strip());
        requireOne(directories.updateById(directory));
        audit.success("file-directory",id,"UPDATE");
        return FileDirectoryView.from(directory);
    }

    @Transactional
    public void delete(String id,int version) {
        var directory=lock(id);
        if(directory.getVersion()!=version) throw new ApiException(409,"目录版本已变化");
        var records=files.directoryFiles(id, ScopeContext.get().loginId());
        if(records.size()>1000 || references.count(id,directory.getScopeId())!=records.size()) {
            throw new ApiException(409,"目录包含不可移动的文件，或超过1000条，请先分批移动");
        }
        for(var file:records) {
            requireOne(files.move(file.getId(),file.getVersion(),null,ScopeContext.get().loginId()));
        }
        requireOne(directories.remove(id,version,ScopeContext.get().loginId()));
        audit.success("file-directory",id,"DELETE");
    }

    /** 保存前锁定选中的个人目录，和删除目录互斥。null 是默认目录。 */
    public void requireTarget(String id) { if(id!=null) lock(id); }

    public void requireReadable(String id) {
        var directory=directories.selectById(id);
        if(directory==null || !directory.getCreatedBy().equals(ScopeContext.get().loginId())) {
            throw new ApiException(404,"目录不存在或不可访问");
        }
    }

    private FileDirectory lock(String id) {
        if(id.isBlank()) throw new ApiException(400,"目录编号不能为空字符串");
        var directory=directories.lockOwned(id,ScopeContext.get().loginId());
        if(directory==null) throw new ApiException(404,"目录不存在或不可访问");
        return directory;
    }
    private static void requireOne(int count) { if(count!=1) throw new ApiException(409,"目录或文件已变化，请刷新后重试"); }
}
