package com.cyys.admin.notice.dto;

import java.time.LocalDateTime;

public record OrgNoticeReadView(String userId,String userName,LocalDateTime firstReadAt) {}
