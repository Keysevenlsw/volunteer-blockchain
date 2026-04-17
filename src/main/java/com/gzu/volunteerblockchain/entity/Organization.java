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
@TableName("organizations")
public class Organization {

    @TableId(value = "organization_id", type = IdType.AUTO)
    private Integer organizationId;

    @TableField("organization_name")
    private String organizationName;

    @TableField("organization_description")
    private String organizationDescription;

    @TableField("avatar_path")
    private String avatarPath;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
