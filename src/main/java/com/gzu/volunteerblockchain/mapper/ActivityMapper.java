package com.gzu.volunteerblockchain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gzu.volunteerblockchain.entity.Activity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {

    @Update("""
        UPDATE activities
        SET current_participants = current_participants + 1
        WHERE activity_id = #{activityId}
          AND current_participants < max_participants
        """)
    int incrementCurrentParticipants(@Param("activityId") Integer activityId);

    @Update("""
        UPDATE activities
        SET current_participants = CASE
            WHEN current_participants > 0 THEN current_participants - 1
            ELSE 0
        END
        WHERE activity_id = #{activityId}
        """)
    int decrementCurrentParticipants(@Param("activityId") Integer activityId);
}
