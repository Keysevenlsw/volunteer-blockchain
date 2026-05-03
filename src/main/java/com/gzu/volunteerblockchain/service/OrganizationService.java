package com.gzu.volunteerblockchain.service;

import com.gzu.volunteerblockchain.dto.PlatformRequests;
import com.gzu.volunteerblockchain.vo.PlatformVOs;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface OrganizationService {

    List<PlatformVOs.OrganizationVO> listOrganizations();

    PlatformVOs.JoinRequestVO createJoinRequest(PlatformRequests.JoinOrganizationRequest request);

    List<PlatformVOs.JoinRequestVO> listMyJoinRequests();

    PlatformVOs.OrganizationVO getMyOrganization();

    void leaveMyOrganization();

    PlatformVOs.OrganizationVO getWorkbenchOrganization();

    PlatformVOs.OrganizationVO updateWorkbenchOrganization(PlatformRequests.OrganizationProfileUpdateRequest request);

    PlatformVOs.OrganizationVO updateWorkbenchOrganizationAvatar(MultipartFile file);

    List<PlatformVOs.JoinRequestVO> listOrganizationJoinRequests(String status);

    PlatformVOs.JoinRequestVO reviewJoinRequest(Integer id, PlatformRequests.ReviewJoinRequest request);
}
