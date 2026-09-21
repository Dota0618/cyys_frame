package com.cyys.admin.notice.dto;

import com.cyys.common.cache.CacheValue;

import java.time.LocalDateTime;

public record OrgNoticeView(String id, String departmentId,
        @CacheValue(cache="department", key="departmentId", field="name") String departmentName,
        String title, String type,
        @CacheValue(cache="dictionary-item", variant="NOTICE_TYPE", key="type", field="label") String typeLabel,
        @CacheValue(cache="dictionary-item", variant="NOTICE_TYPE", key="type", field="disabled") Boolean typeDisabled,
        int version, long readCount, boolean read, LocalDateTime createdAt) {}
