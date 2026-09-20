package com.cyys.admin.notice.dto;

import java.time.LocalDateTime;

public record OrgNoticeView(String id,String departmentId,String departmentName,String title,String type,String typeLabel,boolean typeDisabled,int version,long readCount,boolean read,LocalDateTime createdAt) {}
