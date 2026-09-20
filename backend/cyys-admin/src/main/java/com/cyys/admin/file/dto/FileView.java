package com.cyys.admin.file.dto;

import com.cyys.admin.file.model.FileEntry;
import com.cyys.common.satoken.ScopeContext;
import java.time.LocalDateTime;

public record FileView(String id, String name, Long byteSize, String state, int version,
                       LocalDateTime createdAt, boolean resumable, boolean ownUpload, String directoryId) {
    public static FileView from(FileEntry file) {
        return new FileView(file.getId(), file.getName(), file.getByteSize(), file.getState(), file.getVersion(),
                file.getCreatedAt(), file.getPartBytes() != null, file.getCreatedBy().equals(ScopeContext.get().loginId()), file.getDirId());
    }
}
