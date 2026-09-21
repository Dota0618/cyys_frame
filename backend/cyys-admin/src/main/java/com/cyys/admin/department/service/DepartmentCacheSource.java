package com.cyys.admin.department.service;

import com.cyys.admin.cache.service.BusinessCacheGroups;
import com.cyys.admin.department.dto.DepartmentLabel;
import com.cyys.admin.department.mapper.DepartmentDisplayMapper;
import com.cyys.common.cache.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DepartmentCacheSource implements CacheValueSource<DepartmentLabel> {
    private final RecordCache cache;
    private final DepartmentDisplayMapper mapper;
    public String name() { return "department"; }
    public CacheScope scope() { return CacheScope.UNIT; }
    public Class<DepartmentLabel> recordType() { return DepartmentLabel.class; }
    public Map<String,DepartmentLabel> records(String scopeId, String variant, List<String> ids) {
        if (!variant.isEmpty()) return Map.of();
        return cache.getOrLoad(BusinessCacheGroups.DEPARTMENT_NAMES, scopeId, ids, DepartmentLabel.class,
                missing -> mapper.labels(scopeId, missing).stream().collect(Collectors.toMap(DepartmentLabel::id, Function.identity())));
    }
}
