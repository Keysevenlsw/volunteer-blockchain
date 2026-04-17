package com.gzu.volunteerblockchain.service;

import com.gzu.volunteerblockchain.dto.PlatformRequests;
import com.gzu.volunteerblockchain.vo.PlatformVOs;
import java.util.List;

public interface OrganizationService {

    List<PlatformVOs.OrganizationVO> listOrganizations();

    PlatformVOs.JoinRequestVO createJoinRequest(PlatformRequests.JoinOrganizationRequest request);

    List<PlatformVOs.JoinRequestVO> listMyJoinRequests();

    List<PlatformVOs.JoinRequestVO> listOrganizationJoinRequests(String status);

    PlatformVOs.JoinRequestVO reviewJoinRequest(Integer id, PlatformRequests.ReviewJoinRequest request);
}
