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
@TableName("activity_publish_reviews")
public class ActivityPublishReview {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("activity_id")
    private Integer activityId;

    @TableField("reviewer_id")
    private Integer reviewerId;

    @TableField("reviewer_role")
    private String reviewerRole;

    @TableField("review_action")
    private String reviewAction;

    @TableField("requested_reward_points")
    private Integer requestedRewardPoints;

    @TableField("approved_reward_points")
    private Integer approvedRewardPoints;

    @TableField("review_note")
    private String reviewNote;

    @TableField("is_escalated")
    private Boolean isEscalated;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
