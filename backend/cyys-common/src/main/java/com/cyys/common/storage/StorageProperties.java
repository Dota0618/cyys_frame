package com.cyys.common.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.LinkedHashMap;
import java.util.Map;

/** 部署配置，不从可编辑的业务配置表读取，也不输出凭据。 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "cyys.storage", ignoreUnknownFields = false)
public class StorageProperties {
    private String active;
    private long maxBytes = 20 * 1024 * 1024;
    private long maxResumableBytes = 1024L * 1024 * 1024;
    private int partBytes = 5 * 1024 * 1024;
    private Map<String, Profile> profiles = new LinkedHashMap<>();

    @Getter
    @Setter
    public static class Profile {
        private String driver;
        private String root;
        private String internalUriPrefix;
        private String endpoint;
        private String region;
        private String bucket;
        private String addressing;
        private String accessKey;
        private String secretKey;
    }
}
