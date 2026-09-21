package com.cyys.admin.notice.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.auth.service.PlatformSecurityService;
import com.cyys.admin.dictionary.dto.DictionaryOption;
import com.cyys.admin.dictionary.service.DictionaryLookupService;
import com.cyys.admin.notice.dto.*;
import com.cyys.admin.notice.mapper.SystemNoticeMapper;
import com.cyys.admin.notice.model.SystemNotice;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SystemNoticeService {
    private static final String TYPES = "SYSTEM_NOTICE_TYPE";
    private final SystemNoticeMapper notices;
    private final DictionaryLookupService dictionaries;
    private final PlatformSecurityService security;
    private final DataAuditService audit;

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public IPage<SystemNoticeView> page(long page, long size, String title) {
        security.authorize("system-notice:read");
        var result = notices.page(new Page<SystemNotice>(page, size), title);
        return result.convert(this::view);
    }

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public SystemNoticeView detail(String id) {
        security.authorize("system-notice:read");
        return view(require(id));
    }

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public List<DictionaryOption> options(String id) {
        security.authorize(id == null ? "system-notice:create" : "system-notice:update");
        if (id != null) require(id);
        return dictionaries.options(TYPES);
    }

    @Transactional
    public SystemNoticeView create(CreateSystemNoticeCommand command) {
        security.authorize("system-notice:create");
        var row = new SystemNotice();
        row.setId(IdWorker.getIdStr());
        row.setTitle(command.title().strip());
        row.setContent(command.content());
        row.setVisible(command.visible());
        setType(row, command.type());
        requireOne(notices.create(row, ScopeContext.get().loginId()));
        audit.platformSuccess("system-notice", row.getId(), "CREATE");
        return view(require(row.getId()));
    }

    @Transactional
    public SystemNoticeView update(String id, UpdateSystemNoticeCommand command) {
        security.authorize("system-notice:update");
        var row = require(id);
        row.setVersion(command.version());
        row.setTitle(command.title().strip());
        row.setContent(command.content());
        row.setVisible(command.visible());
        setType(row, command.type());
        requireOne(notices.update(row, ScopeContext.get().loginId()));
        audit.platformSuccess("system-notice", id, "UPDATE");
        row.setVersion(row.getVersion() + 1);
        return view(row);
    }

    @Transactional
    public void delete(String id, int version) {
        security.authorize("system-notice:delete");
        require(id);
        requireOne(notices.softDelete(id, version, ScopeContext.get().loginId()));
        audit.platformSuccess("system-notice", id, "DELETE");
    }

    private SystemNotice require(String id) {
        var row = notices.detail(id);
        if (row == null) throw new ApiException(404, "系统通知不存在");
        return row;
    }

    private void setType(SystemNotice row, String type) {
        // 可保留本记录已有的失效类型；新记录或改选必须是有效候选。
        if (Objects.equals(row.getType(), type)) return;
        var option = dictionaries.eligible(TYPES, type);
        if (option == null) throw new ApiException(409, "系统通知类型已失效，请重新选择");
        row.setType(type);
        row.setTypeLabel(option.label());
    }

    private SystemNoticeView view(SystemNotice row) {
        return new SystemNoticeView(row.getId(), row.getTitle(), row.getType(),
                null, null,
                row.getContent(), row.isVisible(), row.getVersion(), row.getCreatedAt());
    }

    private static void requireOne(int count) {
        if (count != 1) throw new ApiException(409, "系统通知已变化，请刷新后重试");
    }
}
