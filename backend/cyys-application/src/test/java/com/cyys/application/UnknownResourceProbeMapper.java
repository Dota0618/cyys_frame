package com.cyys.application;

import com.cyys.common.mybatis.ProtectedMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/** 引用未定义资源模型的失败关闭探针。 */
@Mapper
@ProtectedMapper(UnregisteredResource.class)
public interface UnknownResourceProbeMapper {
    @Select("select * from sys_org")
    List<Map<String, Object>> rows();
}

final class UnregisteredResource {}
