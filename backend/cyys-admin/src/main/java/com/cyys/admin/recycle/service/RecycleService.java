package com.cyys.admin.recycle.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cyys.admin.recycle.dto.*;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class RecycleService {
    private final Map<String, RecycleResource> resources;

    public RecycleService(List<RecycleResource> registrations) {
        Map<String, RecycleResource> registered = new TreeMap<>();
        for (var resource : registrations) {
            var definition = resource.definition();
            if (!definition.code().matches("[a-z][a-z0-9-]{0,49}")
                    || registered.putIfAbsent(definition.code(), resource) != null) {
                throw new IllegalArgumentException("回收站资源代码无效或重复");
            }
        }
        resources = Collections.unmodifiableMap(registered);
    }

    public List<RecycleResourceView> resources() {
        return resources.values().stream().map(RecycleResource::definition)
                .filter(definition -> has(definition.readPermission()))
                .map(definition -> new RecycleResourceView(definition.code(), definition.name(),
                        definition.searchLabel(), definition.description(), definition.scopeRequired(),
                        has("recycle:restore") && has(definition.restorePermission()),
                        has("recycle:purge") && has(definition.purgePermission())))
                .toList();
    }

    @Transactional(readOnly=true)
    public IPage<RecycleRecord> page(String code, long page, long size, String search) {
        return require(code).page(page, size, search == null ? null : search.strip());
    }

    @Transactional
    public void restore(String code, RecycleBatchCommand command) {
        var resource = require(code);
        requirePermission(resource.definition().restorePermission());
        for (var row : ordered(command)) resource.restore(row.id(), row.version());
    }

    @Transactional
    public void purge(String code, RecycleBatchCommand command) {
        var resource = require(code);
        requirePermission(resource.definition().purgePermission());
        for (var row : ordered(command)) resource.purge(row.id(), row.version());
    }

    private RecycleResource require(String code) {
        var resource = resources.get(code);
        if (resource == null) throw new ApiException(404, "回收站资源不存在");
        requirePermission(resource.definition().readPermission());
        if (resource.definition().scopeRequired()) ScopeContext.get().requireScopeId();
        return resource;
    }

    private static List<RecycleReference> ordered(RecycleBatchCommand command) {
        var rows = command.records();
        if (rows.stream().map(RecycleReference::id).distinct().count() != rows.size()) {
            throw new ApiException(400, "批次包含重复记录");
        }
        return rows.stream().sorted(Comparator.comparing(RecycleReference::id)).toList();
    }

    private static boolean has(String permission) { return ScopeContext.get().permissions().contains(permission); }
    private static void requirePermission(String permission) {
        if (!has(permission)) throw new ApiException(403, "没有该资源的操作权限");
    }
}
