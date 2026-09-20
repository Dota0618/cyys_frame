package com.cyys.admin.role.mapper;

import com.cyys.common.mybatis.ManualDataScope;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyys.admin.role.model.SysRoleMenu;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@ManualDataScope
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {
}
