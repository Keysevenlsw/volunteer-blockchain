package com.gzu.volunteerblockchain.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class PlatformRequests {

    private PlatformRequests() {
    }

    @Data
    @NoArgsConstructor
    public static class JoinOrganizationRequest {
        @NotNull(message = "请选择组织")
        private Integer organizationId;

        @NotBlank(message = "申请说明不能为空")
        @Size(max = 50, message = "申请说明不能超过 50 字")
        private String applyReason;
    }

    @Data
    @NoArgsConstructor
    public static class ReviewJoinRequest {
        @NotBlank(message = "审核状态不能为空")
        private String status;

        @Size(max = 500, message = "审核备注不能超过 500 字")
        private String reviewNote;
    }

    @Data
    @NoArgsConstructor
    public static class OrganizationProfileUpdateRequest {
        @NotBlank(message = "组织名称不能为空")
        @Size(max = 255, message = "组织名称不能超过 255 字")
        private String organizationName;

        @Size(max = 5000, message = "组织简介不能超过 5000 字")
        private String organizationDescription;
    }

    @Data
    @NoArgsConstructor
    public static class ActivitySaveRequest {
        @NotBlank(message = "活动名称不能为空")
        @Size(max = 255, message = "活动名称长度不能超过 255")
        private String activityName;

        @Size(max = 3000, message = "活动描述不能超过 3000 字")
        private String description;

        @NotNull(message = "开始时间不能为空")
        private LocalDateTime startDate;

        @NotNull(message = "结束时间不能为空")
        private LocalDateTime endDate;

        @NotBlank(message = "活动地点不能为空")
        @Size(max = 255, message = "活动地点长度不能超过 255")
        private String location;

        @Size(max = 255, message = "联系人姓名长度不能超过 255")
        private String contactName;

        @Size(max = 20, message = "联系人电话长度不能超过 20")
        private String contactPhone;

        @Size(max = 255, message = "标签长度不能超过 255")
        private String categoryTags;

        @Size(max = 255, message = "图片路径长度不能超过 255")
        private String imagePath;

        @NotNull(message = "最大参与人数不能为空")
        @Min(value = 1, message = "最大参与人数至少为 1")
        private Integer maxParticipants;

        @NotNull(message = "申请积分不能为空")
        @Min(value = 0, message = "申请积分不能小于 0")
        private Integer requestedRewardPoints;

        @Future(message = "报名截止时间必须晚于当前时间")
        private LocalDateTime enrollDeadline;
    }

    @Data
    @NoArgsConstructor
    public static class CompletionSubmitRequest {
        @NotNull(message = "活动 ID 不能为空")
        private Integer activityId;

        @NotBlank(message = "服务地点不能为空")
        @Size(max = 255, message = "服务地点长度不能超过 255")
        private String serviceLocation;

        @NotNull(message = "服务开始时间不能为空")
        private LocalDateTime serviceStartTime;

        @NotNull(message = "服务结束时间不能为空")
        private LocalDateTime serviceEndTime;

        @NotBlank(message = "报告总结不能为空")
        @Size(max = 3000, message = "报告总结不能超过 3000 字")
        private String reportText;

        @NotBlank(message = "贡献内容不能为空")
        @Size(max = 3000, message = "贡献内容不能超过 3000 字")
        private String contributionDetails;

        private List<String> attachmentPaths;
    }

    @Data
    @NoArgsConstructor
    public static class CompletionReviewRequest {
        @NotBlank(message = "审核状态不能为空")
        private String status;

        @Size(max = 500, message = "审核备注不能超过 500 字")
        private String reviewNote;
    }

    @Data
    @NoArgsConstructor
    public static class ProductSaveRequest {
        @NotBlank(message = "商品名称不能为空")
        @Size(max = 255, message = "商品名称长度不能超过 255")
        private String productName;

        @Size(max = 3000, message = "商品描述不能超过 3000 字")
        private String productDescription;

        @NotNull(message = "积分价格不能为空")
        @Min(value = 1, message = "积分价格至少为 1")
        private Integer price;

        @NotNull(message = "库存不能为空")
        @Min(value = 0, message = "库存不能小于 0")
        private Integer stock;

        @Size(max = 255, message = "图片路径长度不能超过 255")
        private String imagePath;
    }

    @Data
    @NoArgsConstructor
    public static class RedemptionRequest {
        @NotNull(message = "商品 ID 不能为空")
        private Integer productId;
    }

    @Data
    @NoArgsConstructor
    public static class RedemptionStatusRequest {
        @NotBlank(message = "兑换状态不能为空")
        private String status;
    }
}
