package com.gzu.volunteerblockchain.service;

import com.gzu.volunteerblockchain.dto.PlatformRequests;
import com.gzu.volunteerblockchain.vo.PlatformVOs;
import java.util.List;

public interface ActivityService {

    PlatformVOs.ActivityVO createActivity(PlatformRequests.ActivitySaveRequest request);

    PlatformVOs.ActivityVO updateActivity(Integer id, PlatformRequests.ActivitySaveRequest request);

    void deleteActivity(Integer id);

    PlatformVOs.ActivityVO submitActivity(Integer id);

    List<PlatformVOs.ActivityVO> listVolunteerActivities();

    List<PlatformVOs.ActivityVO> listOrganizationActivities();

    PlatformVOs.ParticipationVO joinActivity(Integer activityId);

    void cancelParticipation(Integer activityId);

    List<PlatformVOs.ParticipationVO> listMyParticipations();

    PlatformVOs.CompletionVO submitCompletion(PlatformRequests.CompletionSubmitRequest request);

    List<PlatformVOs.CompletionVO> listMyCompletions();

    List<PlatformVOs.CompletionVO> listCompletionReviews(String status);

    PlatformVOs.CompletionVO reviewCompletion(Integer completionId, PlatformRequests.CompletionReviewRequest request);

    List<PlatformVOs.PointsRecordVO> listMyPointsRecords();

    PlatformVOs.PointsBalanceVO getMyPointsBalance();

    PlatformVOs.EvidenceVO getEvidence(String bizType, Integer bizId);

    List<PlatformVOs.EvidenceVO> listOrganizationEvidences();

    PlatformVOs.EvidenceVO retryEvidence(Integer id);
}
