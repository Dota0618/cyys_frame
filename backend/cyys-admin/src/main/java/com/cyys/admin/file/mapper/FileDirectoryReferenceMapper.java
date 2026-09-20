package com.cyys.admin.file.mapper;

import com.cyys.common.mybatis.ManualDataScope;
import org.apache.ibatis.annotations.*;

/** 调用方先锁定并授权个人目录；完整计数防止删除目录时只移动可见子集。 */
@Mapper
@ManualDataScope
public interface FileDirectoryReferenceMapper {
    @Select("select count(*) from sys_file_entry where dir_id=#{directory} and scope_id=#{scope}")
    long count(@Param("directory") String directory, @Param("scope") String scope);
}
