package com.cyys.admin.file.dto;

import com.cyys.admin.file.model.FileDirectory;

public record FileDirectoryView(String id, String name, int version) {
    public static FileDirectoryView from(FileDirectory value) {
        return new FileDirectoryView(value.getId(), value.getName(), value.getVersion());
    }
}
