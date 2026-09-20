package com.cyys.admin.file.mapper;

import com.cyys.admin.file.dto.AttachmentView;
import com.cyys.admin.file.model.FileEntry;
import com.cyys.admin.file.model.AttachmentResource;
import com.cyys.common.mybatis.ManualDataScope;
import org.apache.ibatis.annotations.*;
import java.util.List;

/** 父业务先按当前权限锁定；关联 SQL 固定单位/登记资源/记录，文件写入另由文件行锁串行。 */
@Mapper
@ManualDataScope
public interface FileBindingMapper {
    @Select("select file_id from sys_file_binding where scope_id=#{scope} and resource=#{resource} and record_id=#{record} order by sort,file_id")
    List<String> ids(@Param("scope") String scope,@Param("resource") AttachmentResource resource,@Param("record") String record);

    @Select("select f.id,f.name,f.byte_size,(f.deleted=0 and f.state='READY') as available from sys_file_binding b join sys_file_entry f on f.id=b.file_id and f.scope_id=b.scope_id where b.scope_id=#{scope} and b.resource=#{resource} and b.record_id=#{record} order by b.sort,b.file_id")
    List<AttachmentView> views(@Param("scope") String scope,@Param("resource") AttachmentResource resource,@Param("record") String record);

    @Select("select * from sys_file_entry where scope_id=#{scope} and id=#{id} for update")
    FileEntry lockExisting(@Param("scope") String scope,@Param("id") String id);

    @Delete("delete from sys_file_binding where scope_id=#{scope} and resource=#{resource} and record_id=#{record}")
    int clear(@Param("scope") String scope,@Param("resource") AttachmentResource resource,@Param("record") String record);

    @Insert("insert into sys_file_binding(scope_id,resource,record_id,file_id,sort) values(#{scope},#{resource},#{record},#{file},#{sort})")
    int bind(@Param("scope") String scope,@Param("resource") AttachmentResource resource,@Param("record") String record,@Param("file") String file,@Param("sort") int sort);

    @Select("select file_id from sys_file_binding where scope_id=#{scope} and file_id=#{file} limit 1 for update")
    String firstReference(@Param("scope") String scope,@Param("file") String file);

    @Select("select f.* from sys_file_binding b join sys_file_entry f on f.scope_id=b.scope_id and f.id=b.file_id where b.scope_id=#{scope} and b.resource=#{resource} and b.record_id=#{record} and b.file_id=#{file} and f.deleted=0 and f.state='READY'")
    FileEntry attached(@Param("scope") String scope,@Param("resource") AttachmentResource resource,@Param("record") String record,@Param("file") String file);
}
