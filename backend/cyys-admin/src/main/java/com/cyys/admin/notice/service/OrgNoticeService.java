package com.cyys.admin.notice.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.department.mapper.SysOrgMapper;
import com.cyys.admin.dictionary.dto.DictionaryOption;
import com.cyys.admin.dictionary.service.DictionaryLookupService;
import com.cyys.admin.file.dto.FileOption;
import com.cyys.admin.file.model.AttachmentResource;
import com.cyys.admin.file.service.FileAttachmentService;
import com.cyys.admin.file.service.FileService;
import com.cyys.admin.notice.dto.*;
import com.cyys.admin.notice.mapper.*;
import com.cyys.admin.notice.model.OrgNotice;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrgNoticeService {
    private static final String TYPES="NOTICE_TYPE";
    private final OrgNoticeMapper notices;
    private final OrgNoticeReadMapper reads;
    private final SysOrgMapper departments;
    private final DictionaryLookupService dictionaries;
    private final FileAttachmentService attachments;
    private final DataAuditService audit;

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public IPage<OrgNoticeView> page(long page,long size,String title) {
        var paging=new Page<OrgNotice>(page,size); paging.setOptimizeCountSql(false);
        var result=notices.selectPage(paging,new QueryWrapper<OrgNotice>()
                .like(title!=null&&!title.isBlank(),"title",title).orderByDesc("created_at","id"));
        var views=views(result.getRecords());
        return result.convert(notice -> views.get(notice.getId()));
    }

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public OrgNoticeDetail detail(String id) { return detail(require(id,false)); }

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public OrgNoticeOptions options(String id) {
        if(id!=null) require(id,false);
        return new OrgNoticeOptions(id==null?departments.parentOptionsForCreate():List.of(),dictionaries.options(TYPES));
    }

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public IPage<FileOption> fileOptions(String id,long page,long size,String search) {
        if(id!=null) require(id,false);
        return attachments.options(page,size,search);
    }

    @Transactional
    public OrgNoticeDetail create(CreateOrgNoticeCommand command) {
        if(departments.forReference(command.departmentId())==null) throw new ApiException(409,"通知所属部门不可选择");
        var notice=new OrgNotice(); notice.setOrgId(command.departmentId());
        notice.setTitle(command.title().strip()); notice.setContent(command.content());
        setType(notice,command.type());
        requireOne(notices.insert(notice));
        if(notices.selectById(notice.getId())==null) throw new ApiException(403,"没有该部门的通知创建权限");
        attachments.replace(AttachmentResource.ORG_NOTICE,notice.getId(),command.attachments());
        audit.success("org-notice",notice.getId(),"CREATE");
        return detail(notice);
    }

    @Transactional
    public OrgNoticeDetail update(String id,UpdateOrgNoticeCommand command) {
        var notice=require(id,true);
        if(!notice.getVersion().equals(command.version())) throw new ApiException(409,"通知版本已变化，请刷新后重试");
        notice.setTitle(command.title().strip()); notice.setContent(command.content()); setType(notice,command.type());
        attachments.replace(AttachmentResource.ORG_NOTICE,id,command.attachments());
        requireOne(notices.updateById(notice)); audit.success("org-notice",id,"UPDATE");
        return detail(notice);
    }

    @Transactional
    public void remove(String id,int version) {
        var notice=require(id,true);
        if(notice.getVersion()!=version) throw new ApiException(409,"通知版本已变化");
        attachments.replace(AttachmentResource.ORG_NOTICE,id,List.of());
        requireOne(notices.remove(id,version,ScopeContext.get().loginId()));
        audit.success("org-notice",id,"DELETE");
    }

    @Transactional
    public void markRead(String id) {
        var notice=notices.forRead(id);
        if(notice==null) throw new ApiException(404,"通知不存在或不可访问");
        reads.read(notice.getScopeId(),id,ScopeContext.get().loginId());
    }

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public IPage<OrgNoticeReadView> readers(String id,long page,long size) {
        var notice=require(id,false); var paging=new Page<OrgNoticeReadView>(page,size); paging.setOptimizeCountSql(false);
        return reads.page(paging,notice.getScopeId(),id);
    }

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public FileService.Download download(String id,String file) {
        require(id,false);
        return attachments.download(AttachmentResource.ORG_NOTICE,id,file);
    }

    private OrgNotice require(String id,boolean lock) {
        var notice=lock?notices.lock(id):notices.selectById(id);
        if(notice==null) throw new ApiException(404,"通知不存在或不可访问");
        return notice;
    }
    private void setType(OrgNotice notice,String value) {
        String type=value==null||value.isBlank()?null:value;
        if(Objects.equals(type,notice.getType())) return; // 允许保留已有失效历史值；不能把它选给其他记录。
        DictionaryOption option=type==null?null:dictionaries.eligible(TYPES,type);
        if(type!=null && option==null) throw new ApiException(409,"通知类型已失效，请重新选择");
        notice.setType(type); notice.setTypeLabel(option==null?null:option.label());
    }
    private OrgNoticeDetail detail(OrgNotice notice) {
        return new OrgNoticeDetail(views(List.of(notice)).get(notice.getId()),notice.getContent(),attachments.views(AttachmentResource.ORG_NOTICE,notice.getId()));
    }
    private Map<String,OrgNoticeView> views(List<OrgNotice> rows) {
        if(rows.isEmpty()) return Map.of();
        var identity=ScopeContext.get(); String scope=identity.requireScopeId();
        var counts=reads.counts(scope,rows.stream().map(OrgNotice::getId).toList(),identity.loginId()).stream().collect(Collectors.toMap(OrgNoticeReadMapper.Count::id,Function.identity()));
        var names=reads.departments(scope,rows.stream().map(OrgNotice::getOrgId).distinct().toList()).stream().collect(Collectors.toMap(OrgNoticeReadMapper.DepartmentName::id,OrgNoticeReadMapper.DepartmentName::name));
        var codes=rows.stream().map(OrgNotice::getType).filter(Objects::nonNull).distinct().toList();
        Map<String,DictionaryOption> types=codes.isEmpty()?Map.of():dictionaries.labels(TYPES,codes).stream().collect(Collectors.toMap(DictionaryOption::value,Function.identity()));
        Map<String,OrgNoticeView> result=new HashMap<>();
        for(var row:rows) {
            var count=counts.get(row.getId()); var type=row.getType()==null?null:types.get(row.getType());
            result.put(row.getId(),new OrgNoticeView(row.getId(),row.getOrgId(),names.getOrDefault(row.getOrgId(),row.getOrgId()),
                    row.getTitle(),row.getType(),type==null?row.getTypeLabel():type.label(),row.getType()!=null&&(type==null||type.disabled()),row.getVersion(),
                    count==null?0:count.total(),count!=null&&count.seen(),row.getCreatedAt()));
        }
        return result;
    }
    private static void requireOne(int count) { if(count!=1) throw new ApiException(409,"通知状态或权限已变化"); }
}
