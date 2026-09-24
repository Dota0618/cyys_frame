package com.cyys.admin.recycle.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.auth.service.RoleRowPolicyProvider;
import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.recycle.dto.*;
import com.cyys.admin.recycle.mapper.DynamicRecycleMapper;
import com.cyys.admin.recycle.mapper.RecycleConfigurationMapper;
import com.cyys.admin.recycle.model.RecycleConfiguration;
import com.cyys.common.mybatis.DataPolicyRegistry;
import com.cyys.common.mybatis.DataResource;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

/** 把数据库登记的资源适配为统一回收站接口。 */
@Service
@RequiredArgsConstructor
public class ConfiguredRecycleResourceService {
    private final RecycleConfigurationMapper configurations;
    private final DynamicRecycleMapper mapper;
    private final RoleRowPolicyProvider rowPolicy;
    private final DataAuditService audit;

    public List<RecycleResource> active() {
        return configurations.all().stream().filter(row -> row.getEnabled() == 1)
                .map(this::resource).toList();
    }

    public RecycleResource find(String code) {
        var row = configurations.enabledByCode(code);
        return row == null ? null : resource(row);
    }

    public List<RecycleConfigurationView> views() {
        return configurations.all().stream().map(this::view).toList();
    }

    private RecycleResource resource(RecycleConfiguration row) {
        return new ConfiguredRecycleResource(row, mapper, rowPolicy, audit);
    }

    private RecycleConfigurationView view(RecycleConfiguration row) {
        String prefix = "recycle:" + row.getCode();
        return new RecycleConfigurationView(row.getId(), row.getCode(), row.getName(), row.getTableName(),
                row.getTitleColumn(), row.getSearchColumn(), row.getScopeRequired() == 1,
                row.getScopeColumn(), row.getDepartmentColumn(), row.getAncestorsColumn(), row.getActorColumn(),
                row.getVersionColumn(), row.getDeletedColumn(), row.getCreatedAtColumn(), row.getUpdatedAtColumn(),
                row.getUpdatedByColumn(), row.getSort(), row.getVersion(), row.getEnabled() == 1,
                prefix + ":read", prefix + ":restore", prefix + ":purge");
    }

    private static final class ConfiguredRecycleResource implements RecycleResource {
        private final RecycleConfiguration definition;
        private final DynamicRecycleMapper mapper;
        private final RoleRowPolicyProvider rowPolicy;
        private final DataAuditService audit;

        private ConfiguredRecycleResource(RecycleConfiguration definition, DynamicRecycleMapper mapper,
                                          RoleRowPolicyProvider rowPolicy, DataAuditService audit) {
            this.definition = definition;
            this.mapper = mapper;
            this.rowPolicy = rowPolicy;
            this.audit = audit;
        }

        @Override
        public RecycleDefinition definition() {
            String prefix = "recycle:" + definition.getCode();
            return new RecycleDefinition(definition.getCode(), definition.getName(), definition.getSearchColumn(),
                    "后台登记的 " + definition.getTableName() + " 回收数据", definition.getScopeRequired() == 1,
                    prefix + ":read", prefix + ":restore", prefix + ":purge");
        }

        @Override
        public IPage<RecycleRecord> page(long page, long size, String search) {
            return mapper.page(new Page<>(page, size), definition, predicate(), search);
        }

        @Override
        public void restore(String id, int version) {
            var row = mapper.lock(definition, predicate(), id);
            if (row == null) throw new ApiException(404, "回收记录不存在或已不在当前数据范围");
            if (!Objects.equals(row.version(), version)) throw new ApiException(409, "回收记录版本已变更");
            if (mapper.restore(definition, predicate(), id, version, ScopeContext.get().loginId()) != 1) {
                throw new ApiException(409, "回收记录已变更");
            }
            audit(definition.getCode(), id, "RESTORE");
        }

        @Override
        public void purge(String id, int version) {
            var row = mapper.lock(definition, predicate(), id);
            if (row == null) throw new ApiException(404, "回收记录不存在或已不在当前数据范围");
            if (!Objects.equals(row.version(), version)) throw new ApiException(409, "回收记录版本已变更");
            if (mapper.purge(definition, predicate(), id, version) != 1) throw new ApiException(409, "回收记录已变更");
            audit(definition.getCode(), id, "PURGE");
        }

        private String predicate() {
            if (definition.getScopeRequired() != 1) return "1=1";
            // 资源编码允许使用连字符（例如 org-notice），而行级权限资源标识只接受下划线标识。
            // 业务表名已在登记时按同一规则校验，可作为稳定的内部资源名称。
            var resource = new DataResource(definition.getTableName(), definition.getTableName(), definition.getScopeColumn(),
                    definition.getDepartmentColumn(), definition.getAncestorsColumn(), definition.getActorColumn(),
                    definition.getVersionColumn(), Set.of());
            var policy = new DataPolicyRegistry.Policy(resource, DataPolicyRegistry.Access.READ,
                    DataPolicyRegistry.View.CURRENT_SCOPE);
            var identity = ScopeContext.get();
            rowPolicy.authorize(policy, identity);
            return rowPolicy.predicate(policy, identity, "r");
        }

        private void audit(String code, String id, String operation) {
            if (definition.getScopeRequired() == 1) audit.success("recycle:" + code, id, operation);
            else audit.platformSuccess("recycle:" + code, id, operation);
        }
    }
}
