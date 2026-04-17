package com.gzu.volunteerblockchain.controller;

import com.gzu.volunteerblockchain.common.PermissionAllowed;
import com.gzu.volunteerblockchain.common.PermissionConstants;
import com.gzu.volunteerblockchain.dto.AdminRequests;
import com.gzu.volunteerblockchain.service.AdminService;
import com.gzu.volunteerblockchain.vo.AdminVOs;
import com.gzu.volunteerblockchain.vo.ApiResponse;
import com.gzu.volunteerblockchain.vo.PlatformVOs;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PermissionAllowed(PermissionConstants.ADMIN_USER_LIST)
    @GetMapping("/users")
    public ApiResponse<List<AdminVOs.AdminUserVO>> users() {
        return ApiResponse.success(adminService.listUsers());
    }

    @PermissionAllowed(PermissionConstants.ADMIN_USER_ROLES_VIEW)
    @GetMapping("/users/{userId}/roles")
    public ApiResponse<AdminVOs.UserRolesVO> userRoles(@PathVariable Integer userId) {
        return ApiResponse.success(adminService.getUserRoles(userId));
    }

    @PermissionAllowed(PermissionConstants.ADMIN_USER_ROLES_ASSIGN)
    @PutMapping("/users/{userId}/roles")
    public ApiResponse<AdminVOs.UserRolesVO> assignUserRoles(
        @PathVariable Integer userId,
        @Valid @RequestBody AdminRequests.AssignUserRolesRequest request
    ) {
        return ApiResponse.success("角色分配成功", adminService.assignUserRoles(userId, request));
    }

    @PermissionAllowed(PermissionConstants.ADMIN_ROLE_LIST)
    @GetMapping("/roles")
    public ApiResponse<List<AdminVOs.RoleVO>> roles() {
        return ApiResponse.success(adminService.listRoles());
    }

    @PermissionAllowed(PermissionConstants.ADMIN_ROLE_PERMISSIONS_VIEW)
    @GetMapping("/roles/{roleCode}/permissions")
    public ApiResponse<AdminVOs.RolePermissionsVO> rolePermissions(@PathVariable String roleCode) {
        return ApiResponse.success(adminService.getRolePermissions(roleCode));
    }

    @PermissionAllowed(PermissionConstants.ADMIN_PERMISSION_LIST)
    @GetMapping("/permissions")
    public ApiResponse<List<AdminVOs.PermissionVO>> permissions() {
        return ApiResponse.success(adminService.listPermissions());
    }

    @PermissionAllowed(PermissionConstants.ADMIN_ACTIVITY_REVIEW_LIST)
    @GetMapping("/activity-reviews")
    public ApiResponse<List<AdminVOs.ActivityReviewVO>> activityReviews(
        @RequestParam(value = "status", required = false) String status
    ) {
        return ApiResponse.success(adminService.listActivityReviews(status));
    }

    @PermissionAllowed(PermissionConstants.ADMIN_ACTIVITY_REVIEW_HANDLE)
    @PostMapping("/activity-reviews/{activityId}")
    public ApiResponse<PlatformVOs.ActivityVO> reviewActivity(
        @PathVariable Integer activityId,
        @Valid @RequestBody AdminRequests.ActivityReviewRequest request
    ) {
        return ApiResponse.success("活动审核已处理", adminService.reviewActivity(activityId, request));
    }

    @PermissionAllowed(PermissionConstants.ADMIN_PRODUCT_REVIEW_LIST)
    @GetMapping("/product-reviews")
    public ApiResponse<List<AdminVOs.ProductReviewVO>> productReviews(
        @RequestParam(value = "status", required = false) String status
    ) {
        return ApiResponse.success(adminService.listProductReviews(status));
    }

    @PermissionAllowed(PermissionConstants.ADMIN_PRODUCT_REVIEW_HANDLE)
    @PostMapping("/product-reviews/{productId}")
    public ApiResponse<PlatformVOs.ProductVO> reviewProduct(
        @PathVariable Integer productId,
        @Valid @RequestBody AdminRequests.ProductReviewRequest request
    ) {
        return ApiResponse.success("商品审核已处理", adminService.reviewProduct(productId, request));
    }

    @PermissionAllowed(PermissionConstants.ADMIN_POINTS_RULE_LIST)
    @GetMapping("/points-rules")
    public ApiResponse<List<AdminVOs.PointsRuleVO>> pointsRules() {
        return ApiResponse.success(adminService.listPointsRules());
    }

    @PermissionAllowed(PermissionConstants.ADMIN_POINTS_RULE_CREATE)
    @PostMapping("/points-rules")
    public ApiResponse<AdminVOs.PointsRuleVO> createPointsRule(
        @Valid @RequestBody AdminRequests.PointsRuleSaveRequest request
    ) {
        return ApiResponse.success("积分规则创建成功", adminService.createPointsRule(request));
    }

    @PermissionAllowed(PermissionConstants.ADMIN_POINTS_RULE_UPDATE)
    @PutMapping("/points-rules/{id}")
    public ApiResponse<AdminVOs.PointsRuleVO> updatePointsRule(
        @PathVariable Integer id,
        @Valid @RequestBody AdminRequests.PointsRuleSaveRequest request
    ) {
        return ApiResponse.success("积分规则更新成功", adminService.updatePointsRule(id, request));
    }
    @PermissionAllowed(PermissionConstants.ADMIN_POINTS_SYNC_BALANCES)
    @PostMapping("/points/sync-balances")
    public ApiResponse<AdminVOs.PointsMaintenanceResultVO> syncPointBalances() {
        return ApiResponse.success(adminService.syncPointBalances());
    }

    @PermissionAllowed(PermissionConstants.ADMIN_POINTS_MIGRATE)
    @PostMapping("/points/migrate")
    public ApiResponse<AdminVOs.PointsMaintenanceResultVO> migratePointsToChain() {
        return ApiResponse.success(adminService.migratePointsToChain());
    }
}
