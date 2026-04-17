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
@TableName("products")
public class Product {

    @TableId(value = "product_id", type = IdType.AUTO)
    private Integer productId;

    @TableField("organization_id")
    private Integer organizationId;

    @TableField("product_name")
    private String productName;

    @TableField("product_description")
    private String productDescription;

    @TableField("price")
    private Integer price;

    @TableField("stock")
    private Integer stock;

    @TableField("image_path")
    private String imagePath;

    @TableField("created_by")
    private Integer createdBy;

    @TableField("review_status")
    private String reviewStatus;

    @TableField("review_note")
    private String reviewNote;

    @TableField("reviewed_by")
    private Integer reviewedBy;

    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
