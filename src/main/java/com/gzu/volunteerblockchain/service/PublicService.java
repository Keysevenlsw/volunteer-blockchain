package com.gzu.volunteerblockchain.service;

import com.gzu.volunteerblockchain.vo.InfoItemVO;
import com.gzu.volunteerblockchain.vo.ProjectItemVO;
import java.util.List;

public interface PublicService {

    List<InfoItemVO> getInfoList();

    List<ProjectItemVO> getCompletedProjects(int limit);
}
