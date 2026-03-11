package com.gzu.volunteerblockchain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gzu.volunteerblockchain.entity.Activity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {

    List<Activity> selectCompletedProjects(@Param("limit") int limit);
}
