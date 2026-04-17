package com.gzu.volunteerblockchain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gzu.volunteerblockchain.entity.Organization;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrganizationMapper extends BaseMapper<Organization> {
}
