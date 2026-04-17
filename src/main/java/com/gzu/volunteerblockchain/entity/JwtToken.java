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
@TableName("jwt_tokens")
public class JwtToken {

    @TableId(value = "token_id", type = IdType.AUTO)
    private Integer tokenId;

    @TableField("user_id")
    private Integer userId;

    @TableField("token")
    private String token;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("expires_at")
    private LocalDateTime expiresAt;
}
