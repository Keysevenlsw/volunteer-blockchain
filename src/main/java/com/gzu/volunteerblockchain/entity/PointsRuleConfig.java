package com.gzu.volunteerblockchain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@TableName("points_rule_configs")
public class PointsRuleConfig {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("activity_category")
    private String activityCategory;

    @TableField("min_service_hours")
    private BigDecimal minServiceHours;

    @TableField("max_service_hours")
    private BigDecimal maxServiceHours;

    @TableField("suggested_points")
    private Integer suggestedPoints;

    @TableField("max_points")
    private Integer maxPoints;

    @TableField("escalation_threshold")
    private Integer escalationThreshold;

    @TableField("status")
    private Integer status;

    @TableField("created_by")
    private Integer createdBy;

    @TableField("updated_by")
    private Integer updatedBy;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
