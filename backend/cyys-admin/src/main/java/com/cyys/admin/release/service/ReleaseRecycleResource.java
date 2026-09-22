package com.cyys.admin.release.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.release.mapper.ReleaseMapper;
import com.cyys.admin.release.model.Release;
import com.cyys.admin.recycle.dto.*;
import com.cyys.admin.recycle.service.RecycleResource;
import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.file.service.FileAttachmentService;
import com.cyys.admin.file.model.AttachmentResource;
import com.cyys.admin.auth.service.PlatformSecurityService;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReleaseRecycleResource implements RecycleResource {
    private final ReleaseMapper records;
    private final FileAttachmentService attachments;
    private final DataAuditService audit;

    @Override
    public RecycleDefinition definition() {
        return new RecycleDefinition("release", "版本记录", "版本号",
                "恢复版本内容和已有附件关联；版本记录由全系统共享。",
                false, "release:read", "release:create", "release:delete");
    }

    @Override
    public IPage<RecycleRecord> page(long page, long size, String search) {
        var paging = new Page<RecycleRecord>(page, size);
        paging.setOptimizeCountSql(false);
        return records.recycled(paging, search);
    }

    @Override
    @Transactional(propagation=Propagation.MANDATORY)
    public void restore(String id, int version) {
        require(id, version);
        attachments.validateRecycled(PlatformSecurityService.GLOBAL, AttachmentResource.RELEASE, id);
        requireOne(records.restore(id, version, ScopeContext.get().loginId()));
        audit.platformSuccess("release", id, "RESTORE");
    }

    @Override
    @Transactional(propagation=Propagation.MANDATORY)
    public void purge(String id, int version) {
        require(id, version);
        attachments.clearRecycled(PlatformSecurityService.GLOBAL, AttachmentResource.RELEASE, id);
        requireOne(records.purge(id, version));
        audit.platformSuccess("release", id, "PURGE");
    }

    private Release require(String id, int version) {
        var row = records.lockRecycled(id);
        if (row == null || row.getVersion() != version) {
            throw new ApiException(409, "记录不可访问、已处理或版本已变化，请刷新后重试");
        }
        return row;
    }

    private static void requireOne(int count) {
        if (count != 1) throw new ApiException(409, "记录状态或权限已变化，请刷新后重试");
    }
}
