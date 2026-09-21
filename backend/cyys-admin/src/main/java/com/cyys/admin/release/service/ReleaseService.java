package com.cyys.admin.release.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.auth.service.PlatformSecurityService;
import com.cyys.admin.dictionary.dto.DictionaryOption;
import com.cyys.admin.dictionary.service.DictionaryLookupService;
import com.cyys.admin.file.dto.FileOption;
import com.cyys.admin.file.service.FileAttachmentService;
import com.cyys.admin.file.service.FileService;
import com.cyys.admin.release.dto.*;
import com.cyys.admin.release.mapper.ReleaseMapper;
import com.cyys.admin.release.model.Release;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReleaseService {
    private static final String TYPES = "VERSION_TYPE";
    private final ReleaseMapper releases;
    private final PlatformSecurityService security;
    private final DictionaryLookupService dictionaries;
    private final FileAttachmentService attachments;
    private final DataAuditService audit;

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public IPage<ReleaseView> page(long page,long size,String number) {
        security.authorize("release:read");
        var result=releases.page(new Page<Release>(page,size),number);
        return result.convert(this::view);
    }

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ReleaseDetail detail(String id) {
        security.authorize("release:read");
        return detail(require(id,false));
    }

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public List<DictionaryOption> options(String id) {
        authorizeEdit(id);
        return dictionaries.options(TYPES);
    }

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public IPage<FileOption> fileOptions(String id,long page,long size,String search) {
        authorizeEdit(id);
        security.authorize("file:read");
        security.authorize("file:content:read");
        ScopeContext.get().requireScopeId();
        return attachments.options(page,size,search);
    }

    @Transactional
    public ReleaseDetail create(CreateReleaseCommand command) {
        security.authorize("release:create");
        var row=new Release();
        row.setId(IdWorker.getIdStr());
        row.setReleaseNumber(command.releaseNumber().strip());
        row.setDateUpdate(command.dateUpdate());
        row.setContent(command.content());
        setType(row,command.type());
        requireOne(releases.create(row,ScopeContext.get().loginId()));
        attachments.replaceRelease(row.getId(),command.attachments());
        audit.platformSuccess("release",row.getId(),"CREATE");
        return detail(require(row.getId(),false));
    }

    @Transactional
    public ReleaseDetail update(String id,UpdateReleaseCommand command) {
        security.authorize("release:update");
        var row=require(id,true);
        if(!row.getVersion().equals(command.version())) throw new ApiException(409,"版本记录已变化，请刷新后重试");
        row.setReleaseNumber(command.releaseNumber().strip());
        row.setDateUpdate(command.dateUpdate());
        row.setContent(command.content());
        setType(row,command.type());
        attachments.replaceRelease(id,command.attachments());
        requireOne(releases.update(row,ScopeContext.get().loginId()));
        row.setVersion(row.getVersion()+1);
        audit.platformSuccess("release",id,"UPDATE");
        return detail(row);
    }

    @Transactional
    public void delete(String id,int version) {
        security.authorize("release:delete");
        var row=require(id,true);
        if(row.getVersion()!=version) throw new ApiException(409,"版本记录已变化，请刷新后重试");
        attachments.replaceRelease(id,List.of());
        requireOne(releases.softDelete(id,version,ScopeContext.get().loginId()));
        audit.platformSuccess("release",id,"DELETE");
    }

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public FileService.Download download(String id,String file) {
        security.authorize("release:read");
        require(id,false);
        return attachments.releaseDownload(id,file);
    }

    private void authorizeEdit(String id) {
        security.authorize(id==null?"release:create":"release:update");
        if(id!=null) require(id,false);
    }
    private Release require(String id,boolean lock) {
        var row=lock?releases.lock(id):releases.detail(id);
        if(row==null) throw new ApiException(404,"版本记录不存在");
        return row;
    }
    private void setType(Release row,String value) {
        if(Objects.equals(row.getType(),value)) return;
        var type=dictionaries.eligible(TYPES,value);
        if(type==null) throw new ApiException(409,"版本类型已失效，请重新选择");
        row.setType(value); row.setTypeLabel(type.label());
    }
    private ReleaseDetail detail(Release row) {
        return new ReleaseDetail(view(row),attachments.releaseViews(row.getId()));
    }
    private ReleaseView view(Release row) {
        return new ReleaseView(row.getId(),row.getReleaseNumber(),row.getDateUpdate(),row.getType(),
                null,null,row.getContent(),row.getVersion(),row.getCreatedAt());
    }
    private static void requireOne(int count) {
        if(count!=1) throw new ApiException(409,"版本记录已变化，请刷新后重试");
    }
}
