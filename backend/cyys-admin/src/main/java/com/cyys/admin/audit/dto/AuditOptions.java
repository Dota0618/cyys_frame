package com.cyys.admin.audit.dto;

import com.cyys.admin.dictionary.dto.DictionaryOption;
import com.cyys.common.cache.CacheValue;

import java.util.List;

public record AuditOptions(
        @CacheValue(cache = "dictionary-options", variant = "DATA_AUDIT_OPERATION")
        List<DictionaryOption> operations,
        @CacheValue(cache = "dictionary-options", variant = "DATA_AUDIT_OUTCOME")
        List<DictionaryOption> outcomes) {}
