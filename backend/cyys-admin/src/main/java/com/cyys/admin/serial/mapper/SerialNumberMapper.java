package com.cyys.admin.serial.mapper;

import com.cyys.admin.serial.model.SerialCounter;
import com.cyys.admin.serial.model.SerialRule;
import com.cyys.admin.serial.service.SerialNumberDefinition;
import com.cyys.common.mybatis.ManualDataScope;
import org.apache.ibatis.annotations.*;

/** 仅供受信任取号服务；每条状态语句固定完整单位/业务/周期，不暴露 CRUD 或任意 SQL。 */
@Mapper
@ManualDataScope
public interface SerialNumberMapper {
    @Select("select id from sys_scope where id=#{scope} and status=1 and deleted=0 for share")
    String lockActiveScope(String scope);

    @Insert("insert into sys_serial_rule(scope_id,business_code,prefix,period_type,width,zone_id,permission,created_by) "
            + "values(#{scope},#{code},#{rule.prefix},#{rule.period},#{rule.width},#{rule.zone},#{rule.permission},#{actor}) "
            + "on duplicate key update business_code=business_code")
    int ensureRule(@Param("scope") String scope, @Param("code") String code,
                   @Param("rule") SerialNumberDefinition rule, @Param("actor") String actor);

    @Select("select prefix,period_type,width,zone_id,permission from sys_serial_rule where scope_id=#{scope} and business_code=#{code} for update")
    SerialRule lockRule(@Param("scope") String scope, @Param("code") String code);

    @Insert("insert into sys_serial_counter(id,scope_id,business_code,period_key,sequence_value,updated_by) "
            + "values(#{id},#{scope},#{code},#{period},0,#{actor}) on duplicate key update sequence_value=sequence_value")
    int ensureCounter(@Param("id") String id, @Param("scope") String scope, @Param("code") String code,
                      @Param("period") String period, @Param("actor") String actor);

    @Select("select id,sequence_value from sys_serial_counter where scope_id=#{scope} and business_code=#{code} and period_key=#{period} for update")
    SerialCounter lockCounter(@Param("scope") String scope, @Param("code") String code, @Param("period") String period);

    @Update("update sys_serial_counter set sequence_value=#{next},updated_by=#{actor},updated_at=CURRENT_TIMESTAMP "
            + "where id=#{id} and scope_id=#{scope} and business_code=#{code} and period_key=#{period} and sequence_value=#{previous}")
    int advance(@Param("id") String id, @Param("scope") String scope, @Param("code") String code,
                @Param("period") String period, @Param("previous") long previous, @Param("next") long next, @Param("actor") String actor);
}
