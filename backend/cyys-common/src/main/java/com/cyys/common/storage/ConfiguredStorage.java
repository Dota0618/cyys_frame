package com.cyys.common.storage;

import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ConfiguredStorage {
    private final StorageProperties properties;
    public record Target(String profileId, ObjectStorage storage, long maxBytes) {}

    public static String newKey() { return UUID.randomUUID().toString().replace("-", ""); }

    public Target active() { return resolve(properties.getActive()); }

    /** 历史读取必须传入元数据中的 profileId，不能重新选择 active。 */
    public Target resolve(String profileId) {
        if (profileId == null || !profileId.matches("[a-zA-Z0-9_-]{1,64}")) {
            throw new ApiException(503, "文件存储配置未就绪");
        }
        var profile = properties.getProfiles().get(profileId);
        if (profile == null || properties.getMaxBytes() <= 0) throw new ApiException(503, "文件存储配置未就绪");
        if (!"local".equals(profile.getDriver())) throw new ApiException(503, "所选存储驱动尚未接入");
        if (profile.getRoot() == null || profile.getRoot().isBlank()) throw new ApiException(503, "文件存储配置未就绪");
        try {
            return new Target(profileId, new LocalObjectStorage(Path.of(profile.getRoot())), properties.getMaxBytes());
        } catch (IOException | InvalidPathException error) {
            throw new ApiException(503, "文件存储目录不可用");
        }
    }
}
