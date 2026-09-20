package com.cyys.admin.notice.dto;

import java.time.LocalDateTime;

public record SystemNoticeView(String id, String title, String type, String typeLabel,
                               boolean typeDisabled, String content, boolean visible,
                               int version, LocalDateTime createdAt) {}
