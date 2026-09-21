package com.cyys.admin.scope.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.auth.service.PlatformSecurityService;
import com.cyys.admin.scope.dto.*;
import com.cyys.admin.scope.mapper.ScopeManagementMapper;
import com.cyys.admin.scope.model.SysScope;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;
import java.util.List;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class ScopeManagementService {
    private final PlatformSecurityService security;
    private final ScopeManagementMapper mapper;
    private final DataAuditService audit;

    @Transactional(readOnly=true)
    public List<ScopeView> tree() {
        security.authorize("platform:scope:read");
        return mapper.tree().stream().map(ScopeView::from).toList();
    }

    @Transactional(readOnly=true)
    public IPage<ScopeView> page(int page,int size,String search) {
        security.authorize("platform:scope:read");
        return mapper.page(new Page<SysScope>(page,size),search).convert(ScopeView::from);
    }

    @Transactional
    public ScopeView create(CreateScopeCommand command) {
        security.authorize("platform:scope:create");
        if("GLOBAL".equalsIgnoreCase(command.code())) throw new ApiException(400,"GLOBAL 为平台保留编码");
        var tree=lockTree();
        var row=new SysScope();row.setId(IdWorker.getIdStr());row.setCode(command.code());
        row.setParentId(command.parentId());row.setAncestors(parentPath(command.parentId(),tree,true));
        row.setName(command.name());row.setShortName(command.shortName());row.setContactPerson(command.contactPerson());
        row.setContactPhone(command.contactPhone());row.setRemark(command.remark());row.setStatus(1);row.setSort(0);row.setVersion(0);
        try { requireOne(mapper.create(row,ScopeContext.get().loginId())); }
        catch(DuplicateKeyException failure) { throw new ApiException(409,"机构编码已存在"); }
        audit.platformSuccess("scope",row.getId(),"CREATE");
        return ScopeView.from(row);
    }

    @Transactional
    public ScopeView update(String id,UpdateScopeCommand command) {
        security.authorize("platform:scope:update");
        var tree=lockTree();
        var row=tree.stream().filter(item->item.getId().equals(id)).findFirst().orElse(null);
        if(row==null) throw new ApiException(404,"机构不存在");
        if(!Objects.equals(row.getVersion(),command.version())) throw new ApiException(409,"机构已被修改，请刷新");
        String ancestors=parentPath(command.parentId(),tree,command.enabled());
        if(id.equals(command.parentId())||contains(ancestors,id)) throw new ApiException(400,"不能将机构移动到自身或下级机构");
        var descendants=tree.stream().filter(item->contains(item.getAncestors(),id)).toList();
        if(!command.enabled()&&descendants.stream().anyMatch(item->item.getStatus()==1))
            throw new ApiException(409,"请先停用下级机构");
        String previousPrefix=append(row.getAncestors(),id);
        String nextPrefix=append(ancestors,id);
        if(!ancestors.equals(row.getAncestors())) {
            for(var child:descendants) {
                String previous=child.getAncestors();
                if(!previous.equals(previousPrefix)&&!previous.startsWith(previousPrefix+","))
                    throw new IllegalStateException("机构祖先路径不一致");
                String next=nextPrefix+previous.substring(previousPrefix.length());
                requirePathLength(next);
                requireOne(mapper.updateAncestors(child.getId(),next,ScopeContext.get().loginId()));
            }
        }
        row.setParentId(command.parentId());row.setAncestors(ancestors);
        row.setName(command.name());row.setShortName(command.shortName());row.setContactPerson(command.contactPerson());
        row.setContactPhone(command.contactPhone());row.setRemark(command.remark());row.setStatus(command.enabled()?1:0);row.setSort(command.sort());
        requireOne(mapper.update(row,ScopeContext.get().loginId()));row.setVersion(row.getVersion()+1);
        audit.platformSuccess("scope",id,"UPDATE");return ScopeView.from(row);
    }
    private List<SysScope> lockTree() {
        if(mapper.lockStructure()==null) throw new ApiException(409,"机构管理入口配置缺失");
        return mapper.lockTree();
    }
    private static String parentPath(String parentId,List<SysScope> tree,boolean enabled) {
        if("0".equals(parentId)) return "";
        var parent=tree.stream().filter(item->item.getId().equals(parentId)).findFirst()
                .orElseThrow(()->new ApiException(400,"上级机构不存在"));
        if(enabled&&parent.getStatus()!=1) throw new ApiException(409,"上级机构已停用");
        String path=append(parent.getAncestors(),parent.getId());requirePathLength(path);return path;
    }
    private static String append(String ancestors,String id) {
        return ancestors.isEmpty()?id:ancestors+","+id;
    }
    private static boolean contains(String ancestors,String id) {
        return ancestors!=null&&Arrays.asList(ancestors.split(",")).contains(id);
    }
    private static void requirePathLength(String ancestors) {
        if(ancestors.length()>500) throw new ApiException(400,"机构层级过深");
    }
    private static void requireOne(int count) { if(count!=1) throw new ApiException(409,"单位状态已变更，请刷新"); }
}
