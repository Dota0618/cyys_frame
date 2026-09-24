package com.cyys.admin.recycle.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cyys.admin.recycle.dto.*;

/** 统一回收站资源适配器，由数据库登记配置生成。 */
public interface RecycleResource {
    RecycleDefinition definition();
    IPage<RecycleRecord> page(long page, long size, String search);
    void restore(String id, int version);
    void purge(String id, int version);
}
