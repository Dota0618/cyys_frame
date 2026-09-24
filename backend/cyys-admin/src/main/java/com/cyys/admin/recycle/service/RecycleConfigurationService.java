package com.cyys.admin.recycle.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.menu.mapper.SysMenuMapper;
import com.cyys.admin.menu.model.SysMenu;
import com.cyys.admin.recycle.dto.*;
import com.cyys.admin.recycle.mapper.RecycleConfigurationMapper;
import com.cyys.admin.recycle.model.RecycleConfiguration;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

/** 管理受控软删除表的登记信息，不接受任意 SQL。 */
@Service
@RequiredArgsConstructor
public class RecycleConfigurationService {
    private static final String PARENT = "nav:menu:recycle-page";
    private static final Set<String> TEXT_TYPES = Set.of("char", "varchar", "text", "tinytext", "mediumtext", "longtext");
    private static final Set<String> NUMBER_TYPES = Set.of("tinyint", "smallint", "mediumint", "int", "integer", "bigint", "bit");
    private static final Set<String> TIME_TYPES = Set.of("date", "datetime", "timestamp");
    private static final Set<String> PROTECTED_TABLES = Set.of(
            "sys_scope", "sys_user", "sys_user_scope", "sys_role", "sys_user_role", "sys_menu", "sys_role_menu", "sys_org",
            "sys_user_scope_org", "sys_data_audit", "sys_login_audit", "sys_file_entry", "sys_file_part", "sys_file_dir",
            "sys_org_notice", "sys_org_notice_ref", "sys_file_binding", "sys_notice", "sys_version", "sys_serial_rule",
            "sys_serial_counter", "sys_configure", "param_basic", "param_basic_type", "param_region", "sys_recycle_resource",
            "flyway_schema_history");
    private final RecycleConfigurationMapper mapper;
    private final SysMenuMapper menus;
    private final DataAuditService audit;

    @Transactional(readOnly = true)
    public List<RecycleConfigurationView> all() {
        return mapper.all().stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public List<RecycleTableOption> tables() {
        return mapper.metadata().stream().map(row -> new RecycleTableOption(row.tableName(), parseColumns(row.columnInfo())))
                .filter(this::eligibleTable).toList();
    }

    @Transactional
    public RecycleConfigurationView create(CreateRecycleConfigurationCommand command) {
        String table = clean(command.tableName());
        String code = generatedCode(table);
        validateCode(code);
        validateTable(table, null);
        var fields = fixedFields(command.scopeRequired(), table, command.scopeColumn(), command.departmentColumn(), command.ancestorsColumn(),
                command.actorColumn(), command.versionColumn(), command.deletedColumn(), command.createdAtColumn(), command.updatedAtColumn(), command.updatedByColumn());
        validateColumns(command.scopeRequired(), table, command.titleColumn(), command.searchColumn(), fields);
        var row = new RecycleConfiguration();
        row.setId(IdWorker.getIdStr()); row.setCode(code); row.setName(command.name().strip()); row.setTableName(table);
        apply(row, command.scopeRequired(), command.titleColumn(), command.searchColumn(), fields, command.sort(), 1);
        String actor = actor();
        try {
            if (mapper.create(row, actor) != 1) throw new ApiException(409, "回收站配置写入失败");
            createPermissionMenus(row);
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(409, "回收站编码或业务表已登记");
        }
        audit.platformSuccess("recycle-resource", row.getId(), "CREATE");
        return view(row);
    }

    @Transactional
    public RecycleConfigurationView update(String id, UpdateRecycleConfigurationCommand command) {
        var row = mapper.detail(id);
        if (row == null) throw new ApiException(404, "回收站配置不存在");
        if (!Objects.equals(row.getVersion(), command.version())) throw new ApiException(409, "回收站配置版本已变更");
        var fields = fixedFields(command.scopeRequired(), row.getTableName(), first(command.scopeColumn(), row.getScopeColumn()),
                first(command.departmentColumn(), row.getDepartmentColumn()), first(command.ancestorsColumn(), row.getAncestorsColumn()),
                first(command.actorColumn(), row.getActorColumn()), first(command.versionColumn(), row.getVersionColumn()),
                first(command.deletedColumn(), row.getDeletedColumn()), first(command.createdAtColumn(), row.getCreatedAtColumn()),
                first(command.updatedAtColumn(), row.getUpdatedAtColumn()), first(command.updatedByColumn(), row.getUpdatedByColumn()));
        validateColumns(command.scopeRequired(), row.getTableName(), command.titleColumn(), command.searchColumn(), fields);
        row.setName(command.name().strip());
        apply(row, command.scopeRequired(), command.titleColumn(), command.searchColumn(), fields, command.sort(), command.enabled() ? 1 : 0);
        String actor = actor();
        if (mapper.update(row, actor) != 1) throw new ApiException(409, "回收站配置已变更");
        updatePermissionMenus(row, actor);
        row.setVersion(row.getVersion() + 1);
        audit.platformSuccess("recycle-resource", row.getId(), "UPDATE");
        return view(row);
    }

    @Transactional
    public void delete(String id, int version) {
        var row = mapper.detail(id);
        if (row == null) throw new ApiException(404, "回收站配置不存在");
        if (!Objects.equals(row.getVersion(), version)) throw new ApiException(409, "回收站配置版本已变更");
        String actor = actor();
        if (mapper.softDelete(id, version, actor) != 1) throw new ApiException(409, "回收站配置已变更");
        menus.disableByCode(resourceMenuCode(row.getCode()), actor);
        for (String permission : permissions(row.getCode())) menus.disableByCode(permission, actor);
        audit.platformSuccess("recycle-resource", id, "DELETE");
    }

    private void createPermissionMenus(RecycleConfiguration row) {
        SysMenu parent = menus.byCode(PARENT);
        if (parent == null) throw new ApiException(409, "数据回收站菜单尚未初始化");
        var resourceMenu = new SysMenu();
        resourceMenu.setId(IdWorker.getIdStr()); resourceMenu.setParentId(parent.getId()); resourceMenu.setType(2);
        resourceMenu.setCode(resourceMenuCode(row.getCode())); resourceMenu.setName(row.getName()); resourceMenu.setSort(row.getSort());
        resourceMenu.setVisible(0); resourceMenu.setStatus(1); resourceMenu.setKeepAlive(0); resourceMenu.setAlwaysShow(0);
        try {
            if (menus.create(resourceMenu, actor()) != 1) throw new ApiException(409, "回收站资源目录写入失败");
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(409, "回收站资源目录编码已存在");
        }
        String[] labels = {"查看" + row.getName(), "恢复" + row.getName(), "彻底删除" + row.getName()};
        int sort = 10;
        for (int i = 0; i < 3; i++) {
            var menu = new SysMenu(); menu.setId(IdWorker.getIdStr()); menu.setParentId(resourceMenu.getId()); menu.setType(3);
            menu.setCode(permissions(row.getCode())[i]); menu.setName(labels[i]); menu.setSort(sort + i); menu.setVisible(1); menu.setStatus(1);
            menu.setKeepAlive(0); menu.setAlwaysShow(0);
            try {
                if (menus.create(menu, actor()) != 1) throw new ApiException(409, "回收站权限写入失败");
            } catch (DataIntegrityViolationException exception) {
                throw new ApiException(409, "回收站权限编码已存在");
            }
        }
    }

    private void updatePermissionMenus(RecycleConfiguration row, String actor) {
        String[] labels = {"查看" + row.getName(), "恢复" + row.getName(), "彻底删除" + row.getName()};
        String[] permissions = permissions(row.getCode());
        menus.updateNameByCode(resourceMenuCode(row.getCode()), row.getName(), actor);
        for (int i = 0; i < permissions.length; i++) menus.updateNameByCode(permissions[i], labels[i], actor);
        if (row.getEnabled() == 1) {
            for (String permission : permissions) menus.enableByCode(permission, actor);
            return;
        }
        for (String permission : permissions) menus.disableByCode(permission, actor);
    }

    private void validateCode(String code) {
        if (mapper.idByCode(code) != null) {
            throw new ApiException(409, "回收站编码已存在");
        }
    }

    private void validateTable(String table, String currentId) {
        if (mapper.existingTable(table) == null || PROTECTED_TABLES.contains(table))
            throw new ApiException(400, "只能登记业务表");
        String existing = mapper.idByTable(table);
        if (existing != null && !Objects.equals(existing, currentId)) throw new ApiException(409, "业务表已登记");
    }

    private void validateColumns(Boolean scopeRequired, String table, String title, String search, FixedFields fields) {
        Map<String, String> types = mapper.metadata().stream().filter(r -> r.tableName().equals(table)).findFirst()
                .map(r -> parseColumns(r.columnInfo()).stream().collect(Collectors.toMap(RecycleColumnOption::name, RecycleColumnOption::type)))
                .orElseThrow(() -> new ApiException(400, "业务表元数据不存在"));
        List<String> required = new ArrayList<>(List.of("id", title, search, fields.version(), fields.deleted(), fields.createdAt(), fields.updatedAt(), fields.updatedBy()));
        if (scopeRequired) {
            required.add(fields.scope());
            if (fields.department() != null) required.add(fields.department());
            required.add(fields.actor());
        }
        for (String column : required) if (column == null || !types.containsKey(column)) throw new ApiException(400, "回收站字段不存在: " + column);
        if (!TEXT_TYPES.contains(types.get(title)) || !TEXT_TYPES.contains(types.get(search))) throw new ApiException(400, "标题和搜索字段必须是文本类型");
        if (!NUMBER_TYPES.contains(types.get(fields.version())) || !NUMBER_TYPES.contains(types.get(fields.deleted()))) throw new ApiException(400, "系统版本和删除标记字段类型不符合约定");
        if (!TIME_TYPES.contains(types.get(fields.createdAt())) || !TIME_TYPES.contains(types.get(fields.updatedAt()))) throw new ApiException(400, "系统创建和更新时间字段类型不符合约定");
        if (!TEXT_TYPES.contains(types.get(fields.updatedBy()))) throw new ApiException(400, "系统更新人字段类型不符合约定");
        if (fields.ancestors() != null && !fields.ancestors().isBlank() && !types.containsKey(fields.ancestors())) throw new ApiException(400, "祖先字段不存在");
    }

    private static List<RecycleColumnOption> parseColumns(String info) {
        if (info == null || info.isBlank()) return List.of();
        return Arrays.stream(info.split(",")).map(part -> part.split(":", 3))
                .filter(parts -> parts.length == 3).map(parts -> new RecycleColumnOption(parts[0], parts[1], "1".equals(parts[2]))).toList();
    }

    private static void apply(RecycleConfiguration row, boolean scope, String title, String search, FixedFields fields, Integer sort, int enabled) {
        row.setTitleColumn(title); row.setSearchColumn(search); row.setScopeRequired(scope ? 1 : 0); row.setScopeColumn(fields.scope());
        row.setDepartmentColumn(fields.department()); row.setAncestorsColumn(fields.ancestors()); row.setActorColumn(fields.actor()); row.setVersionColumn(fields.version());
        row.setDeletedColumn(fields.deleted()); row.setCreatedAtColumn(fields.createdAt()); row.setUpdatedAtColumn(fields.updatedAt()); row.setUpdatedByColumn(fields.updatedBy());
        row.setSort(sort == null ? 0 : sort); row.setEnabled(enabled);
    }

    private boolean eligibleTable(RecycleTableOption option) {
        if (PROTECTED_TABLES.contains(option.tableName())) return false;
        var columns = option.columns().stream().map(RecycleColumnOption::name).collect(Collectors.toSet());
        return columns.containsAll(Set.of("id", "version", "deleted", "created_at", "updated_at", "updated_by"));
    }

    private static FixedFields fixedFields(boolean scope, String table, String scopeColumn, String department, String ancestors, String actor,
                                           String version, String deleted, String createdAt, String updatedAt, String updatedBy) {
        return new FixedFields(scope ? first(scopeColumn, "scope_id") : null, scope ? optional(department) : null,
                first(ancestors, "ancestors"), scope ? first(actor, "created_by") : first(actor, "created_by"), first(version, "version"),
                first(deleted, "deleted"), first(createdAt, "created_at"), first(updatedAt, "updated_at"), first(updatedBy, "updated_by"));
    }

    private static String generatedCode(String table) {
        String code = "table-" + table.replace('_', '-');
        if (code.length() <= 50) return code;
        return code.substring(0, 42) + "-" + Integer.toUnsignedString(table.hashCode(), 36);
    }

    private static String first(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private static String optional(String value) { return value == null || value.isBlank() ? null : value; }

    private record FixedFields(String scope, String department, String ancestors, String actor, String version, String deleted,
                               String createdAt, String updatedAt, String updatedBy) {}

    private RecycleConfigurationView view(RecycleConfiguration row) {
        String prefix = "recycle:" + row.getCode();
        return new RecycleConfigurationView(row.getId(), row.getCode(), row.getName(), row.getTableName(), row.getTitleColumn(), row.getSearchColumn(),
                row.getScopeRequired() == 1, row.getScopeColumn(), row.getDepartmentColumn(), row.getAncestorsColumn(), row.getActorColumn(),
                row.getVersionColumn(), row.getDeletedColumn(), row.getCreatedAtColumn(), row.getUpdatedAtColumn(), row.getUpdatedByColumn(),
                row.getSort(), row.getVersion(), row.getEnabled() == 1, prefix + ":read", prefix + ":restore", prefix + ":purge");
    }

    private static String[] permissions(String code) { return new String[]{"recycle:" + code + ":read", "recycle:" + code + ":restore", "recycle:" + code + ":purge"}; }
    private static String resourceMenuCode(String code) { return "recycle:resource:" + code; }
    private static String clean(String value) { return value == null ? null : value.strip(); }
    private static String actor() { return ScopeContext.get().loginId(); }
}
