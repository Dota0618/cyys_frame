package com.cyys.application;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/** 缺少 Mapper 级数据范围边界的失败关闭探针。 */
@Mapper
public interface UnregisteredPolicyProbeMapper {
    @Select("select * from sys_org")
    List<Map<String, Object>> rows();
}
