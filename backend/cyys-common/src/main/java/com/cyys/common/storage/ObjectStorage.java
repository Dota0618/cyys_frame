package com.cyys.common.storage;

import java.io.IOException;
import java.io.InputStream;

/** 仅处理受控对象；身份、元数据和业务关联由调用 Service 负责。 */
public interface ObjectStorage {
    record Written(long bytes, String sha256) {}
    Written write(String key, InputStream source, long maxBytes) throws IOException;
    InputStream read(String key) throws IOException;
    void delete(String key) throws IOException;
}
