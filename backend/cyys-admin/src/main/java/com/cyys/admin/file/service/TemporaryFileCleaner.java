package com.cyys.admin.file.service;

import com.cyys.admin.file.mapper.FilePartMapper;
import com.cyys.admin.file.model.FileEntry;
import com.cyys.common.storage.ConfiguredStorage;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.io.IOException;

/** 调用方必须已经锁定并授权文件；READY 只清理分片，绝不删除完成的主对象。 */
@Service
@RequiredArgsConstructor
public class TemporaryFileCleaner {
    private final FilePartMapper parts;
    private final ConfiguredStorage storage;

    public void clean(FileEntry file) {
        if (!"READY".equals(file.getState()) && !"FAILED".equals(file.getState())) {
            throw new ApiException(409,"上传仍在进行或已进入回收清理");
        }
        var target=storage.resolve(file.getStorageProfile());
        try {
            for (var part:parts.list(file.getId(),file.getScopeId(),file.getCreatedBy())) {
                target.storage().delete(part.objectKey());
            }
            if ("FAILED".equals(file.getState())) target.storage().delete(file.getObjectKey());
        } catch (IOException error) { throw new ApiException(503,"临时文件清理失败，可以重试"); }
    }
}
