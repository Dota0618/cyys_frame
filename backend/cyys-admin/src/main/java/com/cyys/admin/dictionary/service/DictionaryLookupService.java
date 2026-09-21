package com.cyys.admin.dictionary.service;

import com.cyys.admin.cache.service.BusinessCacheGroups;
import com.cyys.admin.dictionary.dto.DictionaryOption;
import com.cyys.admin.dictionary.mapper.DictionaryOptionMapper;
import com.cyys.common.cache.ScopedHashCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;

/** 已授权业务用例的共享字典读取；保存候选仍锁定数据库，不以缓存决定合法性。 */
@Service
@RequiredArgsConstructor
public class DictionaryLookupService {
    private final DictionaryOptionMapper mapper;
    private final ScopedHashCache cache;
    private final JsonMapper json;

    public List<DictionaryOption> options(String name) {
        var result = cache.getOrLoad(BusinessCacheGroups.DICTIONARY_OPTIONS, List.of(name),
                missing -> Map.of(name, json.writeValueAsString(mapper.options(name))));
        try {
            return List.of(json.readValue(result.get(name), DictionaryOption[].class));
        } catch (tools.jackson.core.JacksonException invalid) {
            return mapper.options(name);
        }
    }

    public List<DictionaryOption> labels(String name, List<String> values) {
        if (values.isEmpty()) return List.of();
        Map<String, String> fields = new LinkedHashMap<>();
        values.forEach(value -> fields.put(json.writeValueAsString(List.of(name, value)), value));
        var result = cache.getOrLoad(BusinessCacheGroups.DICTIONARY_LABELS, List.copyOf(fields.keySet()), missing -> {
            Map<String, String> loaded = new LinkedHashMap<>();
            mapper.labels(name, missing.stream().map(fields::get).toList()).forEach(option ->
                    loaded.put(json.writeValueAsString(List.of(name, option.value())), json.writeValueAsString(option)));
            return loaded;
        });
        try {
            return fields.keySet().stream().filter(result::containsKey)
                    .map(field -> json.readValue(result.get(field), DictionaryOption.class)).toList();
        } catch (tools.jackson.core.JacksonException invalid) {
            return mapper.labels(name, values);
        }
    }

    public DictionaryOption eligible(String name, String value) { return mapper.eligible(name, value); }
}
