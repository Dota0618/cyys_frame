package com.cyys.admin.notice.dto;

import com.cyys.common.cache.CacheValue;
import java.time.LocalDateTime;

public record SystemNoticeView(String id, String title, String type,
                               @CacheValue(cache="dictionary-item", variant="SYSTEM_NOTICE_TYPE", key="type", field="label") String typeLabel,
                               @CacheValue(cache="dictionary-item", variant="SYSTEM_NOTICE_TYPE", key="type", field="disabled") Boolean typeDisabled,
                               String content, boolean visible,
                               int version, LocalDateTime createdAt) {}
