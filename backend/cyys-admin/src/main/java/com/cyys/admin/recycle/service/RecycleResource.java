package com.cyys.admin.recycle.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cyys.admin.recycle.dto.*;

/** 业务模块通过 Spring Bean 接入；只使用固定 Mapper，写入加入调用方事务。 */
public interface RecycleResource {
    RecycleDefinition definition();
    IPage<RecycleRecord> page(long page, long size, String search);
    void restore(String id, int version);
    void purge(String id, int version);
}
