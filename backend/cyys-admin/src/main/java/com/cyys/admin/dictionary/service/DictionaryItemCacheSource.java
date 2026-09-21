package com.cyys.admin.dictionary.service;

import com.cyys.admin.dictionary.dto.DictionaryOption;
import com.cyys.common.cache.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DictionaryItemCacheSource implements CacheValueSource<DictionaryOption> {
    private final DictionaryLookupService dictionaries;
    public String name() { return "dictionary-item"; }
    public CacheScope scope() { return CacheScope.SYSTEM; }
    public Class<DictionaryOption> recordType() { return DictionaryOption.class; }
    public Map<String,DictionaryOption> records(String scopeId, String variant, List<String> ids) {
        if (variant.isBlank()) return Map.of();
        return dictionaries.labels(variant, ids).stream().collect(Collectors.toMap(DictionaryOption::value, Function.identity()));
    }
}
