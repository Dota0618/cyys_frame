package com.cyys.admin.scope.mapper;

import com.cyys.admin.scope.dto.ScopeLabel;
import com.cyys.common.mybatis.ManualDataScope;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
@ManualDataScope
public interface ScopeDisplayMapper {
    @Select("<script>select id,name,short_name from sys_scope where deleted=0 and id in "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<ScopeLabel> labels(@Param("ids") List<String> ids);
}
