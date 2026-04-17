package com.gzu.volunteerblockchain.service;

import com.gzu.volunteerblockchain.dto.AdminRequests;
import com.gzu.volunteerblockchain.vo.AdminVOs;
import com.gzu.volunteerblockchain.vo.PlatformVOs;
import java.util.List;

public interface AdminService {

    List<AdminVOs.AdminUserVO> listUsers();

    AdminVOs.UserRolesVO getUserRoles(Integer userId);

    AdminVOs.UserRolesVO assignUserRoles(Integer userId, AdminRequests.AssignUserRolesRequest request);

    List<AdminVOs.RoleVO> listRoles();

    AdminVOs.RolePermissionsVO getRolePermissions(String roleCode);

    List<AdminVOs.PermissionVO> listPermissions();

    List<AdminVOs.ActivityReviewVO> listActivityReviews(String status);

    PlatformVOs.ActivityVO reviewActivity(Integer activityId, AdminRequests.ActivityReviewRequest request);

    List<AdminVOs.ProductReviewVO> listProductReviews(String status);

    PlatformVOs.ProductVO reviewProduct(Integer productId, AdminRequests.ProductReviewRequest request);

    List<AdminVOs.PointsRuleVO> listPointsRules();

    AdminVOs.PointsRuleVO createPointsRule(AdminRequests.PointsRuleSaveRequest request);

    AdminVOs.PointsRuleVO updatePointsRule(Integer id, AdminRequests.PointsRuleSaveRequest request);

    AdminVOs.PointsMaintenanceResultVO syncPointBalances();

    AdminVOs.PointsMaintenanceResultVO migratePointsToChain();
}
