package com.cyys.admin.file.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.file.dto.*;
import com.cyys.admin.file.mapper.*;
import com.cyys.admin.file.model.*;
import com.cyys.admin.auth.service.PlatformSecurityService;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.storage.ConfiguredStorage;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.util.*;

/** 父业务负责自身授权与锁定；此处复核完整附件集合及单位，解除关系不删除对象。 */
@Service
@RequiredArgsConstructor
public class FileAttachmentService {
    private final FileEntryMapper files;
    private final FileBindingMapper bindings;
    private final ConfiguredStorage storage;
    private final PlatformSecurityService platform;

    public IPage<FileOption> options(long page,long size,String search) {
        var paging=new Page<FileEntry>(page,size); paging.setOptimizeCountSql(false);
        return files.selectPage(paging,new QueryWrapper<FileEntry>().eq("state","READY")
                .like(search!=null && !search.isBlank(),"name",search).orderByDesc("created_at","id"))
                .convert(file -> new FileOption(file.getId(),file.getName(),file.getByteSize()));
    }

    @Transactional(propagation=Propagation.MANDATORY)
    public void replace(AttachmentResource resource,String record,List<String> wanted) {
        if(resource != AttachmentResource.ORG_NOTICE) throw new IllegalArgumentException("单位附件资源不匹配");
        replace(ScopeContext.get().requireScopeId(),resource,record,wanted,false);
    }

    /** 已授权并锁定的版本发布流程使用；新增共享关联须有平台目标单位操作权限。 */
    @Transactional(propagation=Propagation.MANDATORY)
    public void replaceRelease(String record,List<String> wanted) {
        replace(PlatformSecurityService.GLOBAL,AttachmentResource.RELEASE,record,wanted,true);
    }

    private void replace(String scope,AttachmentResource resource,String record,List<String> wanted,boolean shared) {
        if(wanted.size()>10 || new HashSet<>(wanted).size()!=wanted.size()) throw new ApiException(400,"附件最多10个，且不能重复");
        var existing=bindings.ids(scope,resource,record);
        if(shared && wanted.stream().anyMatch(id -> !existing.contains(id))) {
            platform.authorize("file:read");
            platform.authorize("file:content:read");
            ScopeContext.get().requireScopeId();
        }
        Map<String,String> fileScopes=new HashMap<>();
        var ordered=new TreeSet<>(existing); ordered.addAll(wanted);
        for(String id:ordered) {
            // 已绑定的附件由父业务授权，可保留或解除；新增附件按当前操作的数据范围重新选择。
            var file=existing.contains(id) ? bindings.lockExisting(bindings.fileScope(scope,resource,record,id),id) : files.lock(id);
            if(file==null || (!shared && !scope.equals(file.getScopeId()))) throw new ApiException(409,"附件不存在或不可选择");
            if(wanted.contains(id) && (!"READY".equals(file.getState()) || file.getDeleted()!=0)) throw new ApiException(409,"附件尚未完成或已删除");
            fileScopes.put(id,file.getScopeId());
        }
        bindings.clear(scope,resource,record);
        for(int i=0;i<wanted.size();i++) if(bindings.bind(scope,resource,record,wanted.get(i),fileScopes.get(wanted.get(i)),i)!=1) throw new ApiException(409,"附件关联保存失败");
    }

    public List<AttachmentView> releaseViews(String record) {
        return bindings.views(PlatformSecurityService.GLOBAL,AttachmentResource.RELEASE,record);
    }

    public FileService.Download releaseDownload(String record,String id) {
        return download(PlatformSecurityService.GLOBAL,AttachmentResource.RELEASE,record,id);
    }

    public List<AttachmentView> views(AttachmentResource resource,String record) {
        return bindings.views(ScopeContext.get().requireScopeId(),resource,record);
    }

    public FileService.Download download(AttachmentResource resource,String record,String id) {
        return download(ScopeContext.get().requireScopeId(),resource,record,id);
    }

    private FileService.Download download(String scope,AttachmentResource resource,String record,String id) {
        var file=bindings.attached(scope,resource,record,id);
        if(file==null) throw new ApiException(404,"附件不存在或已解除关联");
        try { return new FileService.Download(file.getName(),file.getByteSize(),storage.resolve(file.getStorageProfile()).storage().read(file.getObjectKey())); }
        catch(IOException error) { throw new ApiException(503,"附件内容暂不可用"); }
    }

    /** 调用方已锁定并授权回收站父记录；锁定附件后复核可恢复状态。 */
    @Transactional(propagation=Propagation.MANDATORY)
    public void validateRecycled(String scope, AttachmentResource resource, String record) {
        for (String id : new TreeSet<>(bindings.ids(scope, resource, record))) {
            var file = bindings.lockExisting(bindings.fileScope(scope, resource, record, id), id);
            if (file == null || file.getDeleted() != 0 || !"READY".equals(file.getState())) {
                throw new ApiException(409, "关联附件已失效，请先恢复附件后重试");
            }
        }
    }

    /** 只解除已授权父记录的关系，文件对象继续由文件回收站维护。 */
    @Transactional(propagation=Propagation.MANDATORY)
    public void clearRecycled(String scope, AttachmentResource resource, String record) {
        for (String id : new TreeSet<>(bindings.ids(scope, resource, record))) {
            bindings.lockExisting(bindings.fileScope(scope, resource, record, id), id);
        }
        bindings.clear(scope, resource, record);
    }

    @Transactional(propagation=Propagation.MANDATORY)
    public void requireUnbound(FileEntry file) {
        if(bindings.firstReference(file.getScopeId(),file.getId())!=null) throw new ApiException(409,"文件仍被业务引用，请先解除附件关联");
    }
}
