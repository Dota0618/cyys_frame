package com.cyys.admin.release.dto;

import com.cyys.admin.file.dto.AttachmentView;
import java.util.List;

public record ReleaseDetail(ReleaseView release, List<AttachmentView> attachments) {}
