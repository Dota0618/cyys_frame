package com.cyys.admin.file.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.file.dto.*;
import com.cyys.admin.file.mapper.*;
import com.cyys.admin.file.model.*;
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

    public IPage<FileOption> options(long page,long size,String search) {
        var paging=new Page<FileEntry>(page,size); paging.setOptimizeCountSql(false);
        return files.selectPage(paging,new QueryWrapper<FileEntry>().eq("state","READY")
                .like(search!=null && !search.isBlank(),"name",search).orderByDesc("created_at","id"))
                .convert(file -> new FileOption(file.getId(),file.getName(),file.getByteSize()));
    }

    @Transactional(propagation=Propagation.MANDATORY)
    public void replace(AttachmentResource resource,String record,List<String> wanted) {
        if(wanted.size()>10 || new HashSet<>(wanted).size()!=wanted.size()) throw new ApiException(400,"附件最多10个，且不能重复");
        String scope=ScopeContext.get().requireScopeId();
        var existing=bindings.ids(scope,resource,record);
        var ordered=new TreeSet<>(existing); ordered.addAll(wanted);
        for(String id:ordered) {
            // 已绑定的附件由父业务授权，可保留或解除；新增附件按当前操作的数据范围重新选择。
            var file=existing.contains(id) ? bindings.lockExisting(scope,id) : files.lock(id);
            if(file==null || !scope.equals(file.getScopeId())) throw new ApiException(409,"附件不存在或不可选择");
            if(wanted.contains(id) && (!"READY".equals(file.getState()) || file.getDeleted()!=0)) throw new ApiException(409,"附件尚未完成或已删除");
        }
        bindings.clear(scope,resource,record);
        for(int i=0;i<wanted.size();i++) if(bindings.bind(scope,resource,record,wanted.get(i),i)!=1) throw new ApiException(409,"附件关联保存失败");
    }

    public List<AttachmentView> views(AttachmentResource resource,String record) {
        return bindings.views(ScopeContext.get().requireScopeId(),resource,record);
    }

    public FileService.Download download(AttachmentResource resource,String record,String id) {
        var file=bindings.attached(ScopeContext.get().requireScopeId(),resource,record,id);
        if(file==null) throw new ApiException(404,"附件不存在或已解除关联");
        try { return new FileService.Download(file.getName(),file.getByteSize(),storage.resolve(file.getStorageProfile()).storage().read(file.getObjectKey())); }
        catch(IOException error) { throw new ApiException(503,"附件内容暂不可用"); }
    }

    @Transactional(propagation=Propagation.MANDATORY)
    public void requireUnbound(FileEntry file) {
        if(bindings.firstReference(file.getScopeId(),file.getId())!=null) throw new ApiException(409,"文件仍被业务引用，请先解除附件关联");
    }
}
