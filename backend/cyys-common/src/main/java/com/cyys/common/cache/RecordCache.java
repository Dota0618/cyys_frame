package com.cyys.common.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import java.util.function.Function;

/** 在有界 Hash 上保存最小展示 DTO；不支持任意实体、反序列化类名或多态类型。 */
@Component
@RequiredArgsConstructor
public class RecordCache {
    private final ScopedHashCache hashes;
    private final JsonMapper json;

    public <T extends Record> Map<String, T> getOrLoad(CacheRegion region, String scopeId, List<String> ids,
                                                     Class<T> type, Function<List<String>, Map<String,T>> loader) {
        var encoded = hashes.getOrLoad(region, scopeId, ids, missing -> {
            Map<String,String> values = new LinkedHashMap<>();
            loader.apply(missing).forEach((id, row) -> values.put(id, json.writeValueAsString(row)));
            return values;
        });
        Map<String,T> result = new LinkedHashMap<>();
        List<String> corrupt = new ArrayList<>();
        encoded.forEach((id, value) -> {
            try {
                T row = json.readValue(value, type);
                if (row == null) corrupt.add(id); else result.put(id, row);
            } catch (tools.jackson.core.JacksonException invalid) { corrupt.add(id); }
        });
        if (!corrupt.isEmpty()) {
            org.slf4j.LoggerFactory.getLogger(RecordCache.class).warn("Invalid display cache record; falling back to source, region={}", region.id());
            result.putAll(loader.apply(corrupt));
        }
        return Map.copyOf(result);
    }
}
