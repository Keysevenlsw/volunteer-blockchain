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
@TableName("points_records")
public class PointsRecord {

    @TableId(value = "record_id", type = IdType.AUTO)
    private Integer recordId;

    @TableField("user_id")
    private Integer userId;

    @TableField("organization_id")
    private Integer organizationId;

    @TableField("points")
    private Integer points;

    @TableField("transaction_type")
    private String transactionType;

    @TableField("source")
    private String source;

    @TableField("reference_type")
    private String referenceType;

    @TableField("reference_id")
    private Integer referenceId;

    @TableField("biz_key")
    private String bizKey;

    @TableField("digest")
    private String digest;

    @TableField("tx_hash")
    private String txHash;

    @TableField("block_number")
    private String blockNumber;

    @TableField("contract_name")
    private String contractName;

    @TableField("onchain_status")
    private String onchainStatus;

    @TableField("error_message")
    private String errorMessage;

    @TableField("onchain_at")
    private LocalDateTime onchainAt;

    @TableField("chain_balance_after")
    private Integer chainBalanceAfter;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
