package com.cyys.admin.file.mapper;

import com.cyys.common.mybatis.ManualDataScope;
import org.apache.ibatis.annotations.*;
import java.util.List;

/** 先锁定、授权所属文件且确认操作者；每条 SQL 再限定文件、单位和操作者。 */
@Mapper
@ManualDataScope
public interface FilePartMapper {
    record Part(int partIndex, String objectKey, String expectedSha256, boolean ready) {}

    @Insert("insert into sys_file_part(file_id,scope_id,actor_id,part_index,object_key,expected_sha256) "
            + "values(#{file},#{scope},#{actor},#{part.partIndex},#{part.objectKey},#{part.expectedSha256})")
    int insert(@Param("file") String file, @Param("scope") String scope, @Param("actor") String actor, @Param("part") Part part);

    @Select("select part_index,object_key,expected_sha256,ready from sys_file_part "
            + "where file_id=#{file} and scope_id=#{scope} and actor_id=#{actor} order by part_index")
    List<Part> list(@Param("file") String file, @Param("scope") String scope, @Param("actor") String actor);

    @Update("update sys_file_part set ready=1 where file_id=#{file} and scope_id=#{scope} and actor_id=#{actor} and part_index=#{index}")
    int ready(@Param("file") String file, @Param("scope") String scope, @Param("actor") String actor, @Param("index") int index);

    @Update("update sys_file_part set ready=0 where file_id=#{file} and scope_id=#{scope} and actor_id=#{actor} and part_index=#{index}")
    int pending(@Param("file") String file, @Param("scope") String scope, @Param("actor") String actor, @Param("index") int index);
}
