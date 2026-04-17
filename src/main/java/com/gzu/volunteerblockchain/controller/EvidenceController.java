package com.gzu.volunteerblockchain.controller;

import com.gzu.volunteerblockchain.common.RoleAllowed;
import com.gzu.volunteerblockchain.common.RoleConstants;
import com.gzu.volunteerblockchain.service.ActivityService;
import com.gzu.volunteerblockchain.vo.ApiResponse;
import com.gzu.volunteerblockchain.vo.PlatformVOs;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/evidences")
public class EvidenceController {

    private final ActivityService activityService;

    public EvidenceController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @RoleAllowed({
        RoleConstants.VOLUNTEER,
        RoleConstants.ORGANIZATION_ADMIN,
        RoleConstants.ACTIVITY_REVIEWER,
        RoleConstants.PRODUCT_REVIEWER,
        RoleConstants.SYSTEM_ADMIN
    })
    @GetMapping("/{bizType}/{bizId}")
    public ApiResponse<PlatformVOs.EvidenceVO> getEvidence(@PathVariable String bizType, @PathVariable Integer bizId) {
        return ApiResponse.success(activityService.getEvidence(bizType, bizId));
    }

    @RoleAllowed(RoleConstants.ORGANIZATION_ADMIN)
    @GetMapping("/organization")
    public ApiResponse<List<PlatformVOs.EvidenceVO>> organizationEvidences() {
        return ApiResponse.success(activityService.listOrganizationEvidences());
    }

    @RoleAllowed({RoleConstants.ORGANIZATION_ADMIN, RoleConstants.SYSTEM_ADMIN})
    @PostMapping("/{id}/retry")
    public ApiResponse<PlatformVOs.EvidenceVO> retryEvidence(@PathVariable Integer id) {
        return ApiResponse.success("链上存证已发起重试", activityService.retryEvidence(id));
    }
}
