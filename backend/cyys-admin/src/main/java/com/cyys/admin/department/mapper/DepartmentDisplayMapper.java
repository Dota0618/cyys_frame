package com.cyys.admin.department.mapper;

import com.cyys.admin.department.dto.DepartmentLabel;
import com.cyys.common.mybatis.ManualDataScope;
import org.apache.ibatis.annotations.*;
import java.util.List;

/** 已授权响应的部门引用，固定真实单位和引用 ID，不用于候选/写入校验。 */
@Mapper
@ManualDataScope
public interface DepartmentDisplayMapper {
    @Select("<script>select id,name from sys_org where scope_id=#{scope} and deleted=0 and id in "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<DepartmentLabel> labels(@Param("scope") String scope, @Param("ids") List<String> ids);
}
