package com.cyys.admin.notice.dto;

import com.cyys.common.cache.CacheValue;

import java.time.LocalDateTime;

public record OrgNoticeReadView(String userId,
        @CacheValue(cache="user", key="userId", field="name") String userName,
        LocalDateTime firstReadAt) {}
