package com.gzu.volunteerblockchain.service;

import java.time.LocalDateTime;

public interface PointBlockchainService {

    PointChainResult creditPoints(PointChainRequest request);

    PointChainResult debitPoints(PointChainRequest request);

    PointChainResult refundPoints(PointChainRequest request);

    Integer getBalance(Integer userId);

    PointChainTransaction getTransaction(String bizKey);

    record PointChainRequest(
        String bizKey,
        Integer userId,
        Integer organizationId,
        Integer points,
        String source,
        String referenceType,
        Integer referenceId
    ) {
    }

    record PointChainResult(
        String bizKey,
        String transactionType,
        String digest,
        String txHash,
        String blockNumber,
        String contractName,
        Integer chainBalanceAfter,
        LocalDateTime onchainAt
    ) {
    }

    record PointChainTransaction(
        boolean exists,
        String bizKey,
        Integer userId,
        Integer pointsDelta,
        Integer balanceAfter
    ) {
    }
}
