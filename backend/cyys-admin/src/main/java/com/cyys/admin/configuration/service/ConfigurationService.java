package com.cyys.admin.configuration.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.configuration.dto.*;
import com.cyys.admin.configuration.mapper.ConfigureMapper;
import com.cyys.admin.configuration.model.Configure;
import com.cyys.admin.cache.service.BusinessCacheGroups;
import com.cyys.common.cache.CacheGroupService;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import com.cyys.common.export.BoundedXlsx;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ConfigurationService {
    private final ConfigureMapper mapper;
    private final ConfigurationLookupService configurations;
    private final DataAuditService audit;
    private final CacheGroupService cache;

    @Transactional(readOnly = true)
    public IPage<ConfigurationView> page(int page, int size, String name) {
        IPage<Configure> result = mapper.page(new Page<Configure>(page, size), name);
        applyCachedValues(result.getRecords());
        return result.convert(ConfigurationView::from);
    }

    @Transactional(readOnly = true)
    public ConfigurationView detail(String id) {
        Configure row = require(id);
        applyCachedValues(List.of(row));
        return ConfigurationView.from(row);
    }

    @Transactional
    public ConfigurationView create(CreateConfigurationCommand command) {
        var row = new Configure();
        row.setId(IdWorker.getIdStr());
        row.setName(command.name());
        row.setValue(command.value());
        row.setType((command.type() == null ? ConfigurationType.SYSTEM : command.type()).code());
        row.setDesp(command.desp());
        row.setSort(command.sort() == null ? 0 : command.sort());
        validateSystemParameter(row.getName(), row.getValue());
        try {
            requireOne(mapper.create(row, actor()));
        } catch (DuplicateKeyException exception) {
            throw new ApiException(409, "配置名称已存在（包含已删除记录）");
        }
        audit.platformSuccess("configuration", row.getId(), "CREATE");
        cache.invalidateEntriesAfterCommit(BusinessCacheGroups.CONFIGURATIONS, java.util.List.of(row.getName()));
        return ConfigurationView.from(row);
    }

    @Transactional
    public ConfigurationView update(String id, UpdateConfigurationCommand command) {
        Configure row = require(id);
        row.setVersion(command.version());
        row.setValue(command.value());
        row.setType(command.type().code());
        row.setDesp(command.desp());
        row.setSort(command.sort() == null ? 0 : command.sort());
        validateSystemParameter(row.getName(), row.getValue());
        requireOne(mapper.update(row, actor()));
        row.setVersion(command.version() + 1);
        audit.platformSuccess("configuration", id, "UPDATE");
        cache.invalidateEntriesAfterCommit(BusinessCacheGroups.CONFIGURATIONS, java.util.List.of(row.getName()));
        return ConfigurationView.from(row);
    }

    @Transactional
    public void delete(String id, int version) {
        throw new ApiException(409, "全局参数不允许删除，只能修改参数值");
    }

    private Configure require(String id) {
        Configure row = mapper.detail(id);
        if (row == null) throw new ApiException(404, "配置不存在");
        return row;
    }

    private void applyCachedValues(List<Configure> rows) {
        Map<String, String> values = configurations.values(rows.stream().map(Configure::getName).toList());
        rows.forEach(row -> row.setValue(values.get(row.getName())));
    }

    private static void validateSystemParameter(String name, String value) {
        if (value == null || value.isBlank()) return;
        if (ConfigurationKeys.PWD_STRENGTH.equals(name)
                && !Set.of("0", "1", "2").contains(value.strip()))
            throw new ApiException(400, "PWD_STRENGTH 只能设置为 0、1 或 2");
        if (ConfigurationKeys.PWD_LOCK_NUM.equals(name))
            requireNonNegativeInteger(name, value);
        if (ConfigurationKeys.PWD_LOCK_HOUR.equals(name))
            requirePositiveInteger(name, value);
        if (ConfigurationKeys.MAINTENANCE_MODE.equals(name)
                && !Set.of("0", "1").contains(value.strip()))
            throw new ApiException(400, "MAINTENANCE_MODE 只能设置为 0 或 1");
        if (ConfigurationKeys.PASSWORD_EXPIRE_DAYS.equals(name)
                || ConfigurationKeys.AUDIT_RETENTION_DAYS.equals(name))
            requireNonNegativeInteger(name, value);
        if (ConfigurationKeys.FILE_MAX_SIZE_MB.equals(name))
            requirePositiveInteger(name, value);
        if (ConfigurationKeys.EXPORT_MAX_ROWS.equals(name))
            requireIntegerRange(name, value, 1, BoundedXlsx.MAX_ROWS, "必须是 1 到 " + BoundedXlsx.MAX_ROWS + " 的整数");
    }

    private static void requireNonNegativeInteger(String name, String value) {
        try {
            if (Long.parseLong(value.strip()) < 0 || Long.parseLong(value.strip()) > Integer.MAX_VALUE)
                throw new NumberFormatException();
        } catch (NumberFormatException error) {
            throw new ApiException(400, name + " 必须是非负整数");
        }
    }

    private static void requirePositiveInteger(String name, String value) {
        try {
            if (Long.parseLong(value.strip()) <= 0 || Long.parseLong(value.strip()) > Integer.MAX_VALUE)
                throw new NumberFormatException();
        } catch (NumberFormatException error) {
            throw new ApiException(400, name + " 必须是正整数");
        }
    }

    private static void requireIntegerRange(String name, String value, long min, long max, String message) {
        try {
            long parsed = Long.parseLong(value.strip());
            if (parsed < min || parsed > max) throw new NumberFormatException();
        } catch (NumberFormatException error) {
            throw new ApiException(400, name + " " + message);
        }
    }

    private static void requireOne(int count) {
        if (count != 1) throw new ApiException(409, "配置已变更或删除，请刷新后重试");
    }

    private static String actor() { return ScopeContext.get().loginId(); }
}
