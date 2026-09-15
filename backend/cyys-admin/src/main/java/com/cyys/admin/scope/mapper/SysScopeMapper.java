package com.cyys.admin.scope.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyys.admin.scope.model.SysScope;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface SysScopeMapper extends BaseMapper<SysScope> {
    @Select("select * from sys_scope where id = #{id} and status = 1 and deleted = 0")
    SysScope selectEnabledById(@Param("id") String id);

    @Select("select * from sys_scope where status = 1 and deleted = 0 order by sort, id")
    List<SysScope> selectEnabledScopes();
}
