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
@TableName("activities")
public class Activity {

    @TableId(value = "activity_id", type = IdType.AUTO)
    private Integer activityId;

    @TableField("organization_id")
    private Integer organizationId;

    @TableField("activity_name")
    private String activityName;

    @TableField("description")
    private String description;

    @TableField("start_date")
    private LocalDateTime startDate;

    @TableField("end_date")
    private LocalDateTime endDate;

    @TableField("publish_date")
    private LocalDateTime publishDate;

    @TableField("location")
    private String location;

    @TableField("contact_name")
    private String contactName;

    @TableField("contact_phone")
    private String contactPhone;

    @TableField("category_tags")
    private String categoryTags;

    @TableField("image_path")
    private String imagePath;

    @TableField("max_participants")
    private Integer maxParticipants;

    @TableField("current_participants")
    private Integer currentParticipants;

    @TableField("enroll_deadline")
    private LocalDateTime enrollDeadline;

    @TableField("created_by")
    private Integer createdBy;

    @TableField("requested_reward_points")
    private Integer requestedRewardPoints;

    @TableField("approved_reward_points")
    private Integer approvedRewardPoints;

    @TableField("review_status")
    private String reviewStatus;

    @TableField("review_note")
    private String reviewNote;

    @TableField("reviewed_by")
    private Integer reviewedBy;

    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;

    @TableField("status")
    private String status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
