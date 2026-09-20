package com.cyys.admin.notice.dto;

import java.util.List;
import com.cyys.admin.file.dto.AttachmentView;

public record OrgNoticeDetail(OrgNoticeView notice,String content,List<AttachmentView> attachments) {}
