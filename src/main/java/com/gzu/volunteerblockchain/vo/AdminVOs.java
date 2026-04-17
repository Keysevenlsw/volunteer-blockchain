package com.gzu.volunteerblockchain.vo;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class AdminVOs {

    private AdminVOs() {
    }

    @Data
    @NoArgsConstructor
    public static class AdminUserVO {
        private Integer userId;
        private String username;
        private String email;
        private Integer organizationId;
        private String organizationName;
        private String primaryRole;
        private List<String> roles;
        private Integer totalPoints;
    }

    @Data
    @NoArgsConstructor
    public static class UserRolesVO {
        private Integer userId;
        private String primaryRole;
        private List<String> roles;
    }

    @Data
    @NoArgsConstructor
    public static class RoleVO {
        private Integer id;
        private String code;
        private String name;
        private Integer status;
        private Boolean isSystem;
        private String dataScope;
        private Integer sortOrder;
    }

    @Data
    @NoArgsConstructor
    public static class RolePermissionsVO {
        private String roleCode;
        private List<String> permissions;
    }

    @Data
    @NoArgsConstructor
    public static class PermissionVO {
        private Integer id;
        private String code;
        private String name;
        private String module;
        private String httpMethod;
        private String apiPattern;
        private Integer status;
    }

    @Data
    @NoArgsConstructor
    public static class ActivityReviewVO {
        private Integer activityId;
        private Integer organizationId;
        private String organizationName;
        private String activityName;
        private String startDate;
        private String endDate;
        private String location;
        private String categoryTags;
        private Integer requestedRewardPoints;
        private Integer approvedRewardPoints;
        private String reviewStatus;
        private String reviewNote;
        private Integer recommendedPoints;
        private Integer maxPoints;
        private Integer escalationThreshold;
        private Boolean escalationRequired;
        private String submittedBy;
        private String submittedAt;
    }

    @Data
    @NoArgsConstructor
    public static class ProductReviewVO {
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
        private String submittedBy;
        private String submittedAt;
    }

    @Data
    @NoArgsConstructor
    public static class PointsRuleVO {
        private Integer id;
        private String activityCategory;
        private BigDecimal minServiceHours;
        private BigDecimal maxServiceHours;
        private Integer suggestedPoints;
        private Integer maxPoints;
        private Integer escalationThreshold;
        private Integer status;
        private String updatedAt;
    }

    @Data
    @NoArgsConstructor
    public static class PointsMaintenanceResultVO {
        private Integer scannedUsers;
        private Integer migratedUsers;
        private Integer syncedUsers;
        private Integer skippedUsers;
        private Integer failedUsers;
        private List<String> messages;
    }
}
