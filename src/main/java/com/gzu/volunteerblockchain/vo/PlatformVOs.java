package com.gzu.volunteerblockchain.vo;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class PlatformVOs {

    private PlatformVOs() {
    }

    @Data
    @NoArgsConstructor
    public static class OrganizationVO {
        private Integer organizationId;
        private String organizationName;
        private String organizationDescription;
        private String avatarPath;
    }

    @Data
    @NoArgsConstructor
    public static class JoinRequestVO {
        private Integer id;
        private Integer userId;
        private String username;
        private Integer organizationId;
        private String organizationName;
        private String applyReason;
        private String status;
        private String reviewNote;
        private String createdAt;
        private String reviewedAt;
    }

    @Data
    @NoArgsConstructor
    public static class ActivityVO {
        private Integer activityId;
        private Integer organizationId;
        private String organizationName;
        private String activityName;
        private String description;
        private String startDate;
        private String endDate;
        private String publishDate;
        private String location;
        private String contactName;
        private String contactPhone;
        private String categoryTags;
        private String imagePath;
        private Integer maxParticipants;
        private Integer currentParticipants;
        private Integer requestedRewardPoints;
        private Integer approvedRewardPoints;
        private String enrollDeadline;
        private String reviewStatus;
        private String reviewNote;
        private String status;
        private Boolean joined;
        private String completionStatus;
    }

    @Data
    @NoArgsConstructor
    public static class ParticipationVO {
        private Integer participationId;
        private Integer activityId;
        private Integer organizationId;
        private String organizationName;
        private String activityName;
        private String location;
        private String participationDate;
        private Integer earnedPoints;
        private String completionStatus;
    }

    @Data
    @NoArgsConstructor
    public static class CompletionVO {
        private Integer completionId;
        private Integer activityId;
        private String activityName;
        private Integer organizationId;
        private String organizationName;
        private Integer userId;
        private String username;
        private String status;
        private String reportText;
        private String contributionDetails;
        private String serviceLocation;
        private String serviceStartTime;
        private String serviceEndTime;
        private List<String> attachmentPaths;
        private String rejectReason;
        private String approvedAt;
        private String submissionDate;
        private Integer evidenceId;
        private String evidenceStatus;
        private String txHash;
        private String digest;
    }

    @Data
    @NoArgsConstructor
    public static class PointsRecordVO {
        private Integer recordId;
        private Integer organizationId;
        private String organizationName;
        private Integer points;
        private String transactionType;
        private String source;
        private String referenceType;
        private Integer referenceId;
        private String bizKey;
        private String digest;
        private String txHash;
        private String blockNumber;
        private String contractName;
        private String onchainStatus;
        private String errorMessage;
        private String onchainAt;
        private Integer chainBalanceAfter;
        private String createdAt;
    }

    @Data
    @NoArgsConstructor
    public static class PointsBalanceVO {
        private Integer userId;
        private Integer chainBalance;
        private Integer cachedBalance;
        private Boolean consistent;
        private String checkedAt;
    }

    @Data
    @NoArgsConstructor
    public static class ProductVO {
        private Integer productId;
        private Integer organizationId;
        private String organizationName;
        private String productName;
        private String productDescription;
        private Integer price;
        private Integer stock;
        private String imagePath;
        private String reviewStatus;
        private String reviewNote;
        private String reviewedAt;
    }

    @Data
    @NoArgsConstructor
    public static class RedemptionVO {
        private Integer id;
        private Integer productId;
        private String productName;
        private Integer organizationId;
        private String organizationName;
        private Integer userId;
        private String username;
        private Integer pointsCost;
        private String status;
        private String createdAt;
        private String handledAt;
    }

    @Data
    @NoArgsConstructor
    public static class EvidenceVO {
        private Integer id;
        private String bizType;
        private Integer bizId;
        private Integer activityId;
        private String activityName;
        private Integer organizationId;
        private String organizationName;
        private Integer userId;
        private String username;
        private String digest;
        private String txHash;
        private String blockNumber;
        private String contractName;
        private String onchainStatus;
        private String errorMessage;
        private String reviewerName;
        private String reviewedAt;
        private String onchainAt;
        private String storagePath;
        private Boolean verified;
    }
}
