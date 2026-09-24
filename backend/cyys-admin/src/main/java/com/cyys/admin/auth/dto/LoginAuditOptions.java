package com.cyys.admin.auth.dto;

import com.cyys.admin.dictionary.dto.DictionaryOption;
import com.cyys.common.cache.CacheValue;

import java.util.List;

public record LoginAuditOptions(
        @CacheValue(cache = "dictionary-options", variant = "LOGIN_AUDIT_OUTCOME")
        List<DictionaryOption> outcomes) {}
