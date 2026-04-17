package com.gzu.volunteerblockchain.controller;

import com.gzu.volunteerblockchain.common.RoleAllowed;
import com.gzu.volunteerblockchain.common.RoleConstants;
import com.gzu.volunteerblockchain.dto.PlatformRequests;
import com.gzu.volunteerblockchain.service.ActivityService;
import com.gzu.volunteerblockchain.vo.ApiResponse;
import com.gzu.volunteerblockchain.vo.PlatformVOs;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @RoleAllowed(RoleConstants.ORGANIZATION_ADMIN)
    @PostMapping("/activities")
    public ApiResponse<PlatformVOs.ActivityVO> createActivity(@Valid @RequestBody PlatformRequests.ActivitySaveRequest request) {
        return ApiResponse.success("活动草稿创建成功", activityService.createActivity(request));
    }

    @RoleAllowed(RoleConstants.ORGANIZATION_ADMIN)
    @PutMapping("/activities/{id}")
    public ApiResponse<PlatformVOs.ActivityVO> updateActivity(
        @PathVariable Integer id,
        @Valid @RequestBody PlatformRequests.ActivitySaveRequest request
    ) {
        return ApiResponse.success("活动更新成功", activityService.updateActivity(id, request));
    }

    @RoleAllowed(RoleConstants.ORGANIZATION_ADMIN)
    @DeleteMapping("/activities/{id}")
    public ApiResponse<Void> deleteActivity(@PathVariable Integer id) {
        activityService.deleteActivity(id);
        return ApiResponse.success("活动删除成功", null);
    }

    @RoleAllowed(RoleConstants.ORGANIZATION_ADMIN)
    @PostMapping("/activities/{id}/submit")
    public ApiResponse<PlatformVOs.ActivityVO> submitActivity(@PathVariable Integer id) {
        return ApiResponse.success("活动已提交审核", activityService.submitActivity(id));
    }

    @RoleAllowed(RoleConstants.VOLUNTEER)
    @GetMapping("/activities/volunteer")
    public ApiResponse<List<PlatformVOs.ActivityVO>> volunteerActivities() {
        return ApiResponse.success(activityService.listVolunteerActivities());
    }

    @RoleAllowed(RoleConstants.ORGANIZATION_ADMIN)
    @GetMapping("/activities/organization")
    public ApiResponse<List<PlatformVOs.ActivityVO>> organizationActivities() {
        return ApiResponse.success(activityService.listOrganizationActivities());
    }

    @RoleAllowed(RoleConstants.VOLUNTEER)
    @PostMapping("/participations/{activityId}")
    public ApiResponse<PlatformVOs.ParticipationVO> joinActivity(@PathVariable Integer activityId) {
        return ApiResponse.success("报名成功", activityService.joinActivity(activityId));
    }

    @RoleAllowed(RoleConstants.VOLUNTEER)
    @DeleteMapping("/participations/{activityId}")
    public ApiResponse<Void> cancelParticipation(@PathVariable Integer activityId) {
        activityService.cancelParticipation(activityId);
        return ApiResponse.success("已取消报名", null);
    }

    @RoleAllowed(RoleConstants.VOLUNTEER)
    @GetMapping("/participations/mine")
    public ApiResponse<List<PlatformVOs.ParticipationVO>> myParticipations() {
        return ApiResponse.success(activityService.listMyParticipations());
    }

    @RoleAllowed(RoleConstants.VOLUNTEER)
    @PostMapping("/completions")
    public ApiResponse<PlatformVOs.CompletionVO> submitCompletion(@Valid @RequestBody PlatformRequests.CompletionSubmitRequest request) {
        return ApiResponse.success("完成报告已提交", activityService.submitCompletion(request));
    }

    @RoleAllowed(RoleConstants.VOLUNTEER)
    @GetMapping("/completions/mine")
    public ApiResponse<List<PlatformVOs.CompletionVO>> myCompletions() {
        return ApiResponse.success(activityService.listMyCompletions());
    }

    @RoleAllowed(RoleConstants.ORGANIZATION_ADMIN)
    @GetMapping("/reviews")
    public ApiResponse<List<PlatformVOs.CompletionVO>> completionReviews(
        @RequestParam(value = "status", required = false) String status
    ) {
        return ApiResponse.success(activityService.listCompletionReviews(status));
    }

    @RoleAllowed(RoleConstants.ORGANIZATION_ADMIN)
    @PostMapping("/reviews/{completionId}")
    public ApiResponse<PlatformVOs.CompletionVO> reviewCompletion(
        @PathVariable Integer completionId,
        @Valid @RequestBody PlatformRequests.CompletionReviewRequest request
    ) {
        return ApiResponse.success("审核成功", activityService.reviewCompletion(completionId, request));
    }

    @RoleAllowed(RoleConstants.VOLUNTEER)
    @GetMapping("/points")
    public ApiResponse<List<PlatformVOs.PointsRecordVO>> myPointsRecords() {
        return ApiResponse.success(activityService.listMyPointsRecords());
    }

    @RoleAllowed(RoleConstants.VOLUNTEER)
    @GetMapping("/points/balance")
    public ApiResponse<PlatformVOs.PointsBalanceVO> myPointsBalance() {
        return ApiResponse.success(activityService.getMyPointsBalance());
    }
}
