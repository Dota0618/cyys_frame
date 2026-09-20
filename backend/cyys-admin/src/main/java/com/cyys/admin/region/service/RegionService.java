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
import com.cyys.common.export.BoundedXlsx;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RegionService {
    private final RegionMapper regions;
    private final PlatformSecurityService security;
    private final DataAuditService audit;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public byte[] export(ExportRegionsCommand command) {
        security.authorize("region:export");
        String parentId = blankToNull(command.parentId());
        if (parentId != null) require(parentId, false);
        var rows = regions.export(parentId, command.name(), BoundedXlsx.MAX_ROWS + 1);
        if (rows.size() > BoundedXlsx.MAX_ROWS) throw new ApiException(422, "最多导出 5000 条，请缩小地区或名称筛选范围");
        byte[] result;
        try {
            result = BoundedXlsx.write("地区", List.of("地区名称", "地区编码", "邮编", "级别", "简称", "排序", "经度", "纬度"),
                    rows.stream().map(row -> Arrays.asList(row.getName(), row.getCityCode(), row.getZipCode(),
                            String.valueOf(row.getLevel()), row.getShortName(), String.valueOf(row.getSort()),
                            row.getLongitude() == null ? null : row.getLongitude().toPlainString(),
                            row.getLatitude() == null ? null : row.getLatitude().toPlainString())).toList());
        } catch (IOException failure) { throw new ApiException(503, "导出文件生成失败，请稍后重试"); }
        // READ_COMMITTED 使生成后的复核能看见期间已提交的撤权，不沿用生成前的授权快照。
        security.authorize("region:export");
        audit.platformSuccess("region", IdWorker.getIdStr(), "EXPORT_GENERATED");
        return result;
    }

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
