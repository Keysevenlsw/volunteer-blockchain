package com.gzu.volunteerblockchain.controller;

import com.gzu.volunteerblockchain.common.RoleAllowed;
import com.gzu.volunteerblockchain.common.RoleConstants;
import com.gzu.volunteerblockchain.dto.PlatformRequests;
import com.gzu.volunteerblockchain.service.OrganizationService;
import com.gzu.volunteerblockchain.vo.ApiResponse;
import com.gzu.volunteerblockchain.vo.PlatformVOs;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @RoleAllowed({
        RoleConstants.VOLUNTEER,
        RoleConstants.ORGANIZATION_ADMIN,
        RoleConstants.ACTIVITY_REVIEWER,
        RoleConstants.PRODUCT_REVIEWER,
        RoleConstants.SYSTEM_ADMIN
    })
    @GetMapping("/api/organizations")
    public ApiResponse<List<PlatformVOs.OrganizationVO>> listOrganizations() {
        return ApiResponse.success(organizationService.listOrganizations());
    }

    @RoleAllowed(RoleConstants.VOLUNTEER)
    @PostMapping("/api/join-requests")
    public ApiResponse<PlatformVOs.JoinRequestVO> createJoinRequest(@Valid @RequestBody PlatformRequests.JoinOrganizationRequest request) {
        return ApiResponse.success("申请已提交", organizationService.createJoinRequest(request));
    }

    @RoleAllowed(RoleConstants.VOLUNTEER)
    @GetMapping("/api/join-requests/mine")
    public ApiResponse<List<PlatformVOs.JoinRequestVO>> myJoinRequests() {
        return ApiResponse.success(organizationService.listMyJoinRequests());
    }

    @RoleAllowed(RoleConstants.VOLUNTEER)
    @GetMapping("/api/organizations/mine")
    public ApiResponse<PlatformVOs.OrganizationVO> myOrganization() {
        return ApiResponse.success(organizationService.getMyOrganization());
    }

    @RoleAllowed(RoleConstants.VOLUNTEER)
    @PostMapping("/api/organizations/mine/leave")
    public ApiResponse<Void> leaveMyOrganization() {
        organizationService.leaveMyOrganization();
        return ApiResponse.success("已退出组织", null);
    }

    @RoleAllowed(RoleConstants.ORGANIZATION_ADMIN)
    @GetMapping("/api/organizations/workbench")
    public ApiResponse<PlatformVOs.OrganizationVO> workbenchOrganization() {
        return ApiResponse.success(organizationService.getWorkbenchOrganization());
    }

    @RoleAllowed(RoleConstants.ORGANIZATION_ADMIN)
    @PutMapping("/api/organizations/workbench")
    public ApiResponse<PlatformVOs.OrganizationVO> updateWorkbenchOrganization(
        @Valid @RequestBody PlatformRequests.OrganizationProfileUpdateRequest request
    ) {
        return ApiResponse.success("组织信息已更新", organizationService.updateWorkbenchOrganization(request));
    }

    @RoleAllowed(RoleConstants.ORGANIZATION_ADMIN)
    @PostMapping("/api/organizations/workbench/avatar")
    public ApiResponse<PlatformVOs.OrganizationVO> updateWorkbenchOrganizationAvatar(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success("组织头像已更新", organizationService.updateWorkbenchOrganizationAvatar(file));
    }

    @RoleAllowed(RoleConstants.ORGANIZATION_ADMIN)
    @GetMapping("/api/join-requests/organization")
    public ApiResponse<List<PlatformVOs.JoinRequestVO>> organizationJoinRequests(
        @RequestParam(value = "status", required = false) String status
    ) {
        return ApiResponse.success(organizationService.listOrganizationJoinRequests(status));
    }

    @RoleAllowed(RoleConstants.ORGANIZATION_ADMIN)
    @PostMapping("/api/join-requests/{id}/review")
    public ApiResponse<PlatformVOs.JoinRequestVO> reviewJoinRequest(
        @PathVariable Integer id,
        @Valid @RequestBody PlatformRequests.ReviewJoinRequest request
    ) {
        return ApiResponse.success("审核完成", organizationService.reviewJoinRequest(id, request));
    }
}
