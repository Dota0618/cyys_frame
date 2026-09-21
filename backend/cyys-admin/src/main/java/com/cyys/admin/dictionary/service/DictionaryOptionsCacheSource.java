package com.cyys.admin.dictionary.service;

import com.cyys.admin.dictionary.dto.DictionaryOption;
import com.cyys.common.cache.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DictionaryOptionsCacheSource implements CacheValueSource<DictionaryOption> {
    private final DictionaryLookupService dictionaries;
    public String name() { return "dictionary-options"; }
    public CacheScope scope() { return CacheScope.SYSTEM; }
    public CacheShape shape() { return CacheShape.LIST; }
    public String listKey() { return "value"; }
    public Class<DictionaryOption> recordType() { return DictionaryOption.class; }
    public List<DictionaryOption> list(String scopeId, String variant) {
        return variant.isBlank() ? List.of() : dictionaries.options(variant);
    }
}
