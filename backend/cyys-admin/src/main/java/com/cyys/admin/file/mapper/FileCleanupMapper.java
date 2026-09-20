package com.cyys.admin.file.mapper;

import com.cyys.admin.file.model.FileEntry;
import com.cyys.common.mybatis.ManualDataScope;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.List;

/** 仅后台生命周期维护使用：固定状态谓词，候选有界；每次写入必须携带已锁定文件的真实单位。 */
@Mapper
@ManualDataScope
public interface FileCleanupMapper {
    String DUE="temporary_cleaned_at is null and (cleanup_retry_at is null or cleanup_retry_at &lt;= #{now}) "
            + "and ((state='UPLOADING' and expires_at &lt;= #{now}) or state='FAILED' or (state='READY' and part_bytes is not null))";
    record Candidate(String id,String scopeId) {}

    @Select("<script>select id,scope_id from sys_file_entry where " + DUE
            + " and scope_id!='' and scope_id!='GLOBAL' and exists(select 1 from sys_scope s where s.id=sys_file_entry.scope_id) "
            + "<if test='scope != null'> and scope_id=#{scope}</if> order by coalesce(cleanup_retry_at,created_at),id limit #{limit}</script>")
    List<Candidate> candidates(@Param("scope") String scope,@Param("now") LocalDateTime now,@Param("limit") int limit);

    @Select("<script>select * from sys_file_entry where id=#{id} and scope_id=#{scope} and " + DUE
            + " for update skip locked</script>")
    FileEntry lockDue(@Param("id") String id,@Param("scope") String scope,@Param("now") LocalDateTime now);

    @Update("update sys_file_entry set state='FAILED',version=version+1,updated_by=#{actor},updated_at=#{now} "
            + "where id=#{id} and scope_id=#{scope} and state='UPLOADING' and expires_at<=#{now}")
    int expire(@Param("id") String id,@Param("scope") String scope,@Param("actor") String actor,@Param("now") LocalDateTime now);

    @Update("update sys_file_entry set temporary_cleaned_at=#{now},cleanup_retry_at=null "
            + "where id=#{id} and scope_id=#{scope} and temporary_cleaned_at is null and state in ('READY','FAILED')")
    int cleaned(@Param("id") String id,@Param("scope") String scope,@Param("now") LocalDateTime now);

    @Update("update sys_file_entry set cleanup_retry_at=#{retry} where id=#{id} and scope_id=#{scope} "
            + "and temporary_cleaned_at is null and state in ('READY','FAILED')")
    int retry(@Param("id") String id,@Param("scope") String scope,@Param("retry") LocalDateTime retry);
}
