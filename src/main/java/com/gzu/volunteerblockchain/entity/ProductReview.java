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
@TableName("product_reviews")
public class ProductReview {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("product_id")
    private Integer productId;

    @TableField("reviewer_id")
    private Integer reviewerId;

    @TableField("review_action")
    private String reviewAction;

    @TableField("review_note")
    private String reviewNote;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
