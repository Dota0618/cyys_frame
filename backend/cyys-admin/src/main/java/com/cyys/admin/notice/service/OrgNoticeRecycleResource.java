package com.cyys.admin.notice.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.notice.mapper.OrgNoticeMapper;
import com.cyys.admin.notice.model.OrgNotice;
import com.cyys.admin.recycle.dto.*;
import com.cyys.admin.recycle.service.RecycleResource;
import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.department.mapper.SysOrgMapper;
import com.cyys.admin.file.service.FileAttachmentService;
import com.cyys.admin.file.model.AttachmentResource;
import com.cyys.admin.notice.mapper.OrgNoticeReadMapper;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrgNoticeRecycleResource implements RecycleResource {
    private final OrgNoticeMapper records;
    private final SysOrgMapper departments;
    private final OrgNoticeReadMapper reads;
    private final FileAttachmentService attachments;
    private final DataAuditService audit;

    @Override
    public RecycleDefinition definition() {
        return new RecycleDefinition("org-notice", "单位通知", "标题",
                "恢复到原机构和部门，保留阅读记录及附件；部门停用或删除时不能恢复。",
                true, "org-notice:read", "org-notice:create", "org-notice:delete");
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
        var row = require(id, version);
        var department = departments.forReference(row.getOrgId());
        if (department == null || !department.getScopeId().equals(row.getScopeId())) {
            throw new ApiException(409, "原部门已失效或不可访问，无法恢复通知");
        }
        attachments.validateRecycled(row.getScopeId(), AttachmentResource.ORG_NOTICE, id);
        requireOne(records.restore(id, version, ScopeContext.get().loginId()));
        audit.success("org-notice", id, "RESTORE");
    }

    @Override
    @Transactional(propagation=Propagation.MANDATORY)
    public void purge(String id, int version) {
        var row = require(id, version);
        attachments.clearRecycled(row.getScopeId(), AttachmentResource.ORG_NOTICE, id);
        reads.clear(row.getScopeId(), id);
        requireOne(records.purge(id, version));
        audit.success("org-notice", id, "PURGE");
    }

    private OrgNotice require(String id, int version) {
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
