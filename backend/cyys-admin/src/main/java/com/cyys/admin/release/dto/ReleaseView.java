package com.cyys.admin.release.dto;

import com.cyys.common.cache.CacheValue;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReleaseView(String id, String releaseNumber, LocalDate dateUpdate, String type,
                          @CacheValue(cache="dictionary-item", variant="VERSION_TYPE", key="type", field="label") String typeLabel,
                          @CacheValue(cache="dictionary-item", variant="VERSION_TYPE", key="type", field="disabled") Boolean typeDisabled,
                          String content, int version,
                          LocalDateTime createdAt) {}
