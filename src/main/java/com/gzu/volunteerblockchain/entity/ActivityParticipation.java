package com.gzu.volunteerblockchain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@TableName("activity_participation")
public class ActivityParticipation {

    @TableId(value = "participation_id", type = IdType.AUTO)
    private Integer participationId;

    @TableField("user_id")
    private Integer userId;

    @TableField("activity_id")
    private Integer activityId;

    @TableField("earned_points")
    private Integer earnedPoints;

    @TableField("participation_date")
    private LocalDateTime participationDate;
}
