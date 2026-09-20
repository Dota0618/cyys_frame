package com.cyys.admin.dictionary.mapper;

import com.cyys.admin.dictionary.dto.DictionaryOption;
import com.cyys.common.mybatis.ManualDataScope;
import org.apache.ibatis.annotations.*;
import java.util.List;

/** 由已授权业务用例指定固定字典名称，不提供任意字典管理入口。 */
@Mapper
@ManualDataScope
public interface DictionaryOptionMapper {
    @Select("select t.param_key as value,t.param_value as label,false as disabled from param_basic b join param_basic_type t on t.basic_id=b.id where b.system_name=#{name} and b.deleted=0 and t.deleted=0 order by t.sort,t.id")
    List<DictionaryOption> options(String name);

    @Select("select t.param_key as value,t.param_value as label,false as disabled from param_basic b join param_basic_type t on t.basic_id=b.id where b.system_name=#{name} and b.deleted=0 and t.deleted=0 and t.param_key=#{value} for share")
    DictionaryOption eligible(@Param("name") String name,@Param("value") String value);

    @Select("<script>select t.param_key as value,t.param_value as label,(b.deleted=1 or t.deleted=1) as disabled from param_basic b join param_basic_type t on t.basic_id=b.id where b.system_name=#{name} and t.param_key in <foreach collection='values' item='value' open='(' separator=',' close=')'>#{value}</foreach></script>")
    List<DictionaryOption> labels(@Param("name") String name,@Param("values") List<String> values);
}
