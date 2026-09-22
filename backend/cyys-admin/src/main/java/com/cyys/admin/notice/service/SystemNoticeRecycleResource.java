package com.cyys.admin.notice.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.notice.mapper.SystemNoticeMapper;
import com.cyys.admin.notice.model.SystemNotice;
import com.cyys.admin.recycle.dto.*;
import com.cyys.admin.recycle.service.RecycleResource;
import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemNoticeRecycleResource implements RecycleResource {
    private final SystemNoticeMapper records;
    private final DataAuditService audit;

    @Override
    public RecycleDefinition definition() {
        return new RecycleDefinition("system-notice", "系统通知", "标题",
                "恢复正文、类型和原显示状态；系统通知由全系统共享。",
                false, "system-notice:read", "system-notice:create", "system-notice:delete");
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
        requireOne(records.restore(id, version, ScopeContext.get().loginId()));
        audit.platformSuccess("system-notice", id, "RESTORE");
    }

    @Override
    @Transactional(propagation=Propagation.MANDATORY)
    public void purge(String id, int version) {
        require(id, version);
        requireOne(records.purge(id, version));
        audit.platformSuccess("system-notice", id, "PURGE");
    }

    private SystemNotice require(String id, int version) {
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
