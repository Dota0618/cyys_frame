package com.cyys.admin.configuration.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.auth.service.PlatformSecurityService;
import com.cyys.admin.configuration.dto.*;
import com.cyys.admin.configuration.mapper.ConfigureMapper;
import com.cyys.admin.configuration.model.Configure;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConfigurationService {
    private final ConfigureMapper mapper;
    private final PlatformSecurityService security;
    private final DataAuditService audit;

    @Transactional(readOnly = true)
    public IPage<ConfigurationView> page(int page, int size, String name) {
        security.authorize("configuration:read");
        return mapper.page(new Page<Configure>(page, size), name).convert(ConfigurationView::from);
    }

    @Transactional(readOnly = true)
    public ConfigurationView detail(String id) {
        security.authorize("configuration:read");
        return ConfigurationView.from(require(id));
    }

    @Transactional
    public ConfigurationView create(CreateConfigurationCommand command) {
        security.authorize("configuration:create");
        var row = new Configure();
        row.setId(IdWorker.getIdStr());
        row.setName(command.name());
        row.setValue(command.value());
        row.setType((command.type() == null ? ConfigurationType.SYSTEM : command.type()).code());
        row.setDesp(command.desp());
        row.setSort(command.sort() == null ? 0 : command.sort());
        try {
            requireOne(mapper.create(row, actor()));
        } catch (DuplicateKeyException exception) {
            throw new ApiException(409, "配置名称已存在（包含已删除记录）");
        }
        audit.platformSuccess("configuration", row.getId(), "CREATE");
        return ConfigurationView.from(row);
    }

    @Transactional
    public ConfigurationView update(String id, UpdateConfigurationCommand command) {
        security.authorize("configuration:update");
        Configure row = require(id);
        row.setVersion(command.version());
        row.setValue(command.value());
        row.setType(command.type().code());
        row.setDesp(command.desp());
        row.setSort(command.sort() == null ? 0 : command.sort());
        requireOne(mapper.update(row, actor()));
        row.setVersion(command.version() + 1);
        audit.platformSuccess("configuration", id, "UPDATE");
        return ConfigurationView.from(row);
    }

    @Transactional
    public void delete(String id, int version) {
        security.authorize("configuration:delete");
        requireOne(mapper.softDelete(id, version, actor()));
        audit.platformSuccess("configuration", id, "DELETE");
    }

    private Configure require(String id) {
        Configure row = mapper.detail(id);
        if (row == null) throw new ApiException(404, "配置不存在");
        return row;
    }

    private static void requireOne(int count) {
        if (count != 1) throw new ApiException(409, "配置已变更或删除，请刷新后重试");
    }

    private static String actor() { return ScopeContext.get().loginId(); }
}
