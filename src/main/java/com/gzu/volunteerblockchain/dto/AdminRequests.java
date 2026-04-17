package com.gzu.volunteerblockchain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class AdminRequests {

    private AdminRequests() {
    }

    @Data
    @NoArgsConstructor
    public static class AssignUserRolesRequest {
        @NotEmpty(message = "角色列表不能为空")
        private List<String> roleCodes;
    }

    @Data
    @NoArgsConstructor
    public static class ActivityReviewRequest {
        @NotBlank(message = "审核状态不能为空")
        private String status;

        @Min(value = 0, message = "批准积分不能小于 0")
        private Integer approvedRewardPoints;

        @Size(max = 500, message = "审核备注不能超过 500 字")
        private String reviewNote;
    }

    @Data
    @NoArgsConstructor
    public static class ProductReviewRequest {
        @NotBlank(message = "审核状态不能为空")
        private String status;

        @Size(max = 500, message = "审核备注不能超过 500 字")
        private String reviewNote;
    }

    @Data
    @NoArgsConstructor
    public static class PointsRuleSaveRequest {
        @NotBlank(message = "活动分类不能为空")
        @Size(max = 100, message = "活动分类长度不能超过 100")
        private String activityCategory;

        @NotNull(message = "最小服务时长不能为空")
        @DecimalMin(value = "0.00", inclusive = true, message = "最小服务时长不能小于 0")
        private BigDecimal minServiceHours;

        @NotNull(message = "最大服务时长不能为空")
        @DecimalMin(value = "0.01", inclusive = true, message = "最大服务时长必须大于 0")
        private BigDecimal maxServiceHours;

        @NotNull(message = "建议积分不能为空")
        @Min(value = 0, message = "建议积分不能小于 0")
        private Integer suggestedPoints;

        @NotNull(message = "最高积分不能为空")
        @Min(value = 0, message = "最高积分不能小于 0")
        private Integer maxPoints;

        @NotNull(message = "复审阈值不能为空")
        @Min(value = 0, message = "复审阈值不能小于 0")
        private Integer escalationThreshold;

        @NotNull(message = "状态不能为空")
        private Integer status;
    }
}
