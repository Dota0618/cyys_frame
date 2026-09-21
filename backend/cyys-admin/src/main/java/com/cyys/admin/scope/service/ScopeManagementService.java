package com.cyys.admin.scope.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.auth.service.PlatformSecurityService;
import com.cyys.admin.scope.dto.*;
import com.cyys.admin.scope.mapper.ScopeManagementMapper;
import com.cyys.admin.scope.model.SysScope;
import com.cyys.common.satoken.DataIsolationProperties;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ScopeManagementService {
    private final PlatformSecurityService security;
    private final ScopeManagementMapper mapper;
    private final DataAuditService audit;
    private final DataIsolationProperties isolation;

    @Transactional(readOnly=true)
    public IPage<ScopeView> page(int page,int size,String search) {
        security.authorize("platform:scope:read");
        return mapper.page(new Page<SysScope>(page,size),search).convert(ScopeView::from);
    }

    @Transactional
    public ScopeView create(CreateScopeCommand command) {
        security.authorize("platform:scope:create");
        if("GLOBAL".equalsIgnoreCase(command.code())) throw new ApiException(400,"GLOBAL 为平台保留编码");
        var row=new SysScope();row.setId(IdWorker.getIdStr());row.setCode(command.code());
        row.setName(command.name());row.setShortName(command.shortName());row.setContactPerson(command.contactPerson());
        row.setContactPhone(command.contactPhone());row.setRemark(command.remark());row.setStatus(1);row.setSort(0);row.setVersion(0);
        try { requireOne(mapper.create(row,ScopeContext.get().loginId())); }
        catch(DuplicateKeyException failure) { throw new ApiException(409,"单位编码已存在"); }
        audit.platformSuccess("scope",row.getId(),"CREATE");
        return ScopeView.from(row);
    }

    @Transactional
    public ScopeView update(String id,UpdateScopeCommand command) {
        security.authorize("platform:scope:update");
        var row=mapper.lock(id);
        if(row==null) throw new ApiException(404,"单位不存在");
        if(!Objects.equals(row.getVersion(),command.version())) throw new ApiException(409,"单位已被修改，请刷新");
        if(!command.enabled() && isolation.mode()==DataIsolationProperties.Mode.SINGLE && id.equals(isolation.singleScopeId()))
            throw new ApiException(409,"不能停用当前单单位部署的固定单位");
        if(!command.enabled() && id.equals(ScopeContext.get().scopeId())) throw new ApiException(409,"请先切换到其他单位或全部单位视图，再停用该单位");
        row.setName(command.name());row.setShortName(command.shortName());row.setContactPerson(command.contactPerson());
        row.setContactPhone(command.contactPhone());row.setRemark(command.remark());row.setStatus(command.enabled()?1:0);row.setSort(command.sort());
        requireOne(mapper.update(row,ScopeContext.get().loginId()));row.setVersion(row.getVersion()+1);
        audit.platformSuccess("scope",id,"UPDATE");return ScopeView.from(row);
    }
    private static void requireOne(int count) { if(count!=1) throw new ApiException(409,"单位状态已变更，请刷新"); }
}
