package com.cyys.admin.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyys.admin.file.model.FileDirectory;
import com.cyys.common.mybatis.ProtectedMapper;
import org.apache.ibatis.annotations.*;

@Mapper
@ProtectedMapper(FileDirectory.class)
public interface FileDirectoryMapper extends BaseMapper<FileDirectory> {
    @Select("select * from sys_file_dir where id=#{id} and created_by=#{actor} and deleted=0 for update")
    FileDirectory lockOwned(@Param("id") String id, @Param("actor") String actor);

    @Update("update sys_file_dir set deleted=1,version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP "
            + "where id=#{id} and version=#{version} and created_by=#{actor} and deleted=0")
    int remove(@Param("id") String id, @Param("version") int version, @Param("actor") String actor);
}
