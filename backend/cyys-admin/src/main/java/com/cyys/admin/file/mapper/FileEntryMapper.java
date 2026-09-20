package com.cyys.admin.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyys.admin.file.model.FileEntry;
import com.cyys.common.mybatis.ProtectedMapper;
import org.apache.ibatis.annotations.*;
import java.util.List;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

@Mapper
@ProtectedMapper(FileEntry.class)
public interface FileEntryMapper extends BaseMapper<FileEntry> {
    @Select("select * from sys_file_entry where id=#{id} and deleted=0 for update")
    FileEntry lock(String id);

    @Update("update sys_file_entry set temporary_cleaned_at=CURRENT_TIMESTAMP,cleanup_retry_at=null "
            + "where id=#{id} and version=#{version} and state in ('READY','FAILED')")
    int temporaryCleaned(@Param("id") String id,@Param("version") int version);

    @Select("select * from sys_file_entry where dir_id=#{directory} and created_by=#{actor} order by id limit 1001 for update")
    List<FileEntry> directoryFiles(@Param("directory") String directory, @Param("actor") String actor);

    @Update("update sys_file_entry set dir_id=#{directory},version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP "
            + "where id=#{id} and version=#{version} and created_by=#{actor}")
    int move(@Param("id") String id, @Param("version") int version, @Param("directory") String directory, @Param("actor") String actor);

    @Select("<script>select * from sys_file_entry where deleted=1 and state!='PURGED' "
            + "<if test='search != null and search != &quot;&quot;'>and name like concat('%',#{search},'%')</if> "
            + "order by updated_at desc,id desc</script>")
    IPage<FileEntry> recycle(Page<FileEntry> page, @Param("search") String search);

    @Select("select * from sys_file_entry where id=#{id} and deleted=1 for update")
    FileEntry lockRecycled(String id);

    @Update("update sys_file_entry set deleted=0,dir_id=null,version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP "
            + "where id=#{id} and version=#{version} and deleted=1 and state='READY'")
    int restore(@Param("id") String id, @Param("version") int version, @Param("actor") String actor);

    @Update("update sys_file_entry set state=#{state},version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP "
            + "where id=#{id} and version=#{version} and deleted=1 and state in ('READY','FAILED','PURGING')")
    int purgeState(@Param("id") String id, @Param("version") int version, @Param("state") String state, @Param("actor") String actor);

    @Update("update sys_file_entry set deleted=1, version=version+1, updated_by=#{actor}, updated_at=CURRENT_TIMESTAMP "
            + "where id=#{id} and version=#{version} and deleted=0 and state in ('READY','FAILED')")
    int trash(@Param("id") String id, @Param("version") int version, @Param("actor") String actor);
}
