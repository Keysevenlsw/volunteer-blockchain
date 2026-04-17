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
@TableName("blockchain_evidences")
public class BlockchainEvidence {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("biz_type")
    private String bizType;

    @TableField("biz_id")
    private Integer bizId;

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

    @TableField("storage_path")
    private String storagePath;

    @TableField("reviewer_name")
    private String reviewerName;

    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("onchain_at")
    private LocalDateTime onchainAt;
}
