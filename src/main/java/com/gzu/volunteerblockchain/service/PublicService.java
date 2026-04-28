package com.gzu.volunteerblockchain.service;

import com.gzu.volunteerblockchain.vo.InfoItemVO;
import com.gzu.volunteerblockchain.vo.PlatformVOs;
import com.gzu.volunteerblockchain.vo.ProjectItemVO;
import java.util.List;

public interface PublicService {

    List<InfoItemVO> getInfoList();

    List<ProjectItemVO> getCompletedProjects(int limit);

    List<PlatformVOs.ActivityVO> listPublicActivities(String keyword, Integer limit, Integer organizationId);

    PlatformVOs.ActivityVO getPublicActivity(Integer id);

    List<PlatformVOs.ActivityRegistrationVO> listPublicActivityRegistrations(Integer activityId);

    List<PlatformVOs.OrganizationVO> listPublicOrganizations(String keyword, Integer organizationId);

    PlatformVOs.OrganizationVO getPublicOrganization(Integer id);

    List<PlatformVOs.EvidenceVO> listPublicEvidences(Integer limit, String status, Integer organizationId, String keyword);

    PlatformVOs.EvidenceVO getPublicEvidence(String bizType, Integer bizId);
}
