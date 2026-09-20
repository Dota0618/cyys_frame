package com.cyys.admin.region.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.auth.service.PlatformSecurityService;
import com.cyys.admin.region.dto.*;
import com.cyys.admin.region.mapper.RegionMapper;
import com.cyys.admin.region.model.Region;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RegionService {
    private final RegionMapper regions;
    private final PlatformSecurityService security;
    private final DataAuditService audit;

    @Transactional(readOnly=true)
    public IPage<RegionView> page(long page,long size,String parent,String name) {
        security.authorize("region:read");
        String parentId=blankToNull(parent);
        if(parentId!=null) require(parentId,false);
        var paging=new Page<Region>(page,size); paging.setOptimizeCountSql(false);
        return regions.page(paging,parentId,name).convert(RegionView::from);
    }

    @Transactional(readOnly=true)
    public RegionView detail(String id) {
        security.authorize("region:read");
        return RegionView.from(require(id,false));
    }

    @Transactional
    public RegionView create(CreateRegionCommand command) {
        security.authorize("region:create");
        var row=new Region();
        row.setId(IdWorker.getIdStr()); row.setParentId(blankToNull(command.parentId()));
        row.setLevel(row.getParentId()==null?0:Math.addExact(require(row.getParentId(),true).getLevel(),1));
        row.setCityCode(command.cityCode().strip()); row.setName(command.name().strip());
        row.setShortName(blankToNull(command.shortName())); row.setZipCode(blankToNull(command.zipCode()));
        row.setLongitude(command.longitude()); row.setLatitude(command.latitude());
        row.setSort(command.sort()==null?0:command.sort());
        try { requireOne(regions.create(row,actor())); }
        catch(DuplicateKeyException conflict) { throw new ApiException(409,"地区编码已存在（包含历史记录）"); }
        audit.platformSuccess("region",row.getId(),"CREATE");
        return RegionView.from(row);
    }

    @Transactional
    public RegionView update(String id,UpdateRegionCommand command) {
        security.authorize("region:update");
        var row=require(id,true);
        if(!row.getVersion().equals(command.version())) throw new ApiException(409,"地区已变化，请刷新后重试");
        row.setCityCode(command.cityCode().strip()); row.setName(command.name().strip());
        row.setShortName(blankToNull(command.shortName())); row.setZipCode(blankToNull(command.zipCode()));
        row.setLongitude(command.longitude()); row.setLatitude(command.latitude());
        row.setSort(command.sort()==null?0:command.sort());
        try { requireOne(regions.update(row,actor())); }
        catch(DuplicateKeyException conflict) { throw new ApiException(409,"地区编码已存在（包含历史记录）"); }
        audit.platformSuccess("region",id,"UPDATE");
        return RegionView.from(require(id,false));
    }

    @Transactional
    public void delete(List<RegionVersionReference> commands) {
        security.authorize("region:delete");
        if(commands.isEmpty() || commands.size()>100 || commands.stream().map(RegionVersionReference::id).distinct().count()!=commands.size())
            throw new ApiException(400,"每次删除 1–100 条且不能重复");
        var ordered=commands.stream().sorted(Comparator.comparing(RegionVersionReference::id)).toList();
        for(var command:ordered) {
            var row=require(command.id(),true);
            if(!row.getVersion().equals(command.version())) throw new ApiException(409,"地区已变化，请刷新后重试");
            if(regions.firstChild(row.getId())!=null) throw new ApiException(409,"地区存在下级，不能删除");
        }
        for(var command:ordered) {
            requireOne(regions.softDelete(command.id(),command.version(),actor()));
            audit.platformSuccess("region",command.id(),"DELETE");
        }
    }

    private Region require(String id,boolean lock) {
        var row=lock?regions.lock(id):regions.detail(id);
        if(row==null) throw new ApiException(404,"地区不存在");
        return row;
    }
    private static String blankToNull(String value) { return value==null||value.isBlank()?null:value.strip(); }
    private static String actor() { return ScopeContext.get().loginId(); }
    private static void requireOne(int count) { if(count!=1) throw new ApiException(409,"地区已变化，请刷新后重试"); }
}
