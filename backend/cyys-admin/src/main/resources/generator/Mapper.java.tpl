package @@PACKAGE@@.mapper;

import @@PACKAGE@@.model.@@CLASS@@;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyys.common.mybatis.ProtectedMapper;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
@ProtectedMapper(@@CLASS@@.class)
public interface @@CLASS@@Mapper extends BaseMapper<@@CLASS@@> {
    @Select("select * from @@TABLE@@ where id=#{id} and deleted=0 for update")
    @@CLASS@@ lock(String id);

    @Select("select * from @@TABLE@@ where deleted=0 order by created_at desc,id desc limit 501")
    List<@@CLASS@@> choices();

    @Update("update @@TABLE@@ set deleted=1,version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP where id=#{id} and version=#{version} and deleted=0")
    int remove(@Param("id") String id,@Param("version") int version,@Param("actor") String actor);
}
