package com.gzu.volunteerblockchain.service;

import com.gzu.volunteerblockchain.vo.InfoItemVO;
import com.gzu.volunteerblockchain.vo.PlatformVOs;
import com.gzu.volunteerblockchain.vo.ProjectItemVO;
import java.util.List;

public interface PublicService {

    List<InfoItemVO> getInfoList();

    List<ProjectItemVO> getCompletedProjects(int limit);

    List<PlatformVOs.EvidenceVO> listPublicEvidences(Integer limit, String status, Integer organizationId, String keyword);

    PlatformVOs.EvidenceVO getPublicEvidence(String bizType, Integer bizId);
}
