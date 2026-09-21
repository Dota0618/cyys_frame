package com.cyys.application;

import com.cyys.admin.user.dto.UserLabel;
import com.cyys.admin.dictionary.dto.DictionaryOption;
import com.cyys.common.cache.CacheValue;
import java.util.List;

public record CacheValueProbeView(String userId,
        @CacheValue(cache="user",key="userId",field="name") String name,
        @CacheValue(cache="user",key="userId") UserLabel user,
        String type,
        @CacheValue(cache="dictionary-options",variant="CACHE_TEST_TYPES",key="type",field="label") String typeName,
        @CacheValue(cache="dictionary-options",variant="CACHE_TEST_TYPES",key="type") DictionaryOption typeRecord,
        @CacheValue(cache="dictionary-options",variant="CACHE_TEST_TYPES") List<DictionaryOption> types) {}
