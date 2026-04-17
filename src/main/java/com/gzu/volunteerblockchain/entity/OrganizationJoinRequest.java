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
@TableName("organization_join_requests")
public class OrganizationJoinRequest {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("user_id")
    private Integer userId;

    @TableField("organization_id")
    private Integer organizationId;

    @TableField("apply_reason")
    private String applyReason;

    @TableField("status")
    private String status;

    @TableField("reviewed_by")
    private Integer reviewedBy;

    @TableField("review_note")
    private String reviewNote;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;
}
