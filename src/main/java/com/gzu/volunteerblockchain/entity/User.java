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
@TableName("users")
public class User {

    @TableId(value = "user_id", type = IdType.AUTO)
    private Integer userId;

    @TableField("username")
    private String username;

    @TableField("password_hash")
    private String passwordHash;

    @TableField("email")
    private String email;

    @TableField("avatar_path")
    private String avatarPath;

    @TableField("organization_id")
    private Integer organizationId;

    @TableField("role")
    private String role;

    @TableField("join_date")
    private LocalDateTime joinDate;

    @TableField("total_points")
    private Integer totalPoints;
}
