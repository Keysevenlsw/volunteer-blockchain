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
@TableName("activity_completion")
public class ActivityCompletion {

    @TableId(value = "completion_id", type = IdType.AUTO)
    private Integer completionId;

    @TableField("user_id")
    private Integer userId;

    @TableField("activity_id")
    private Integer activityId;

    @TableField("completion_status")
    private String completionStatus;

    @TableField("report_text")
    private String reportText;

    @TableField("service_location")
    private String serviceLocation;

    @TableField("service_start_time")
    private LocalDateTime serviceStartTime;

    @TableField("service_end_time")
    private LocalDateTime serviceEndTime;

    @TableField("contribution_details")
    private String contributionDetails;

    @TableField("attachment_paths")
    private String attachmentPaths;

    @TableField("reject_reason")
    private String rejectReason;

    @TableField("approved_by")
    private Integer approvedBy;

    @TableField("approved_at")
    private LocalDateTime approvedAt;

    @TableField("submission_date")
    private LocalDateTime submissionDate;
}
