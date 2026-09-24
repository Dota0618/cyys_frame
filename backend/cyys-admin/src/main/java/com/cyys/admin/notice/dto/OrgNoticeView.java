package com.cyys.admin.notice.dto;

import com.cyys.common.cache.CacheValue;

import java.time.LocalDateTime;

public record OrgNoticeView(String id, String title, String type,
        @CacheValue(cache="dictionary-item", variant="NOTICE_TYPE", key="type", field="label") String typeLabel,
        @CacheValue(cache="dictionary-item", variant="NOTICE_TYPE", key="type", field="disabled") Boolean typeDisabled,
        int version, long readCount, boolean read, LocalDateTime createdAt) {}
