package com.gzu.volunteerblockchain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gzu.volunteerblockchain.common.AuthUser;
import com.gzu.volunteerblockchain.common.RoleConstants;
import com.gzu.volunteerblockchain.common.UserContext;
import com.gzu.volunteerblockchain.dto.PlatformRequests;
import com.gzu.volunteerblockchain.entity.Activity;
import com.gzu.volunteerblockchain.entity.ActivityCompletion;
import com.gzu.volunteerblockchain.entity.ActivityParticipation;
import com.gzu.volunteerblockchain.entity.ActivityPublishReview;
import com.gzu.volunteerblockchain.entity.BlockchainEvidence;
import com.gzu.volunteerblockchain.entity.CompletionReview;
import com.gzu.volunteerblockchain.entity.Organization;
import com.gzu.volunteerblockchain.entity.PointsRecord;
import com.gzu.volunteerblockchain.entity.User;
import com.gzu.volunteerblockchain.exception.BusinessException;
import com.gzu.volunteerblockchain.mapper.ActivityCompletionMapper;
import com.gzu.volunteerblockchain.mapper.ActivityMapper;
import com.gzu.volunteerblockchain.mapper.ActivityParticipationMapper;
import com.gzu.volunteerblockchain.mapper.ActivityPublishReviewMapper;
import com.gzu.volunteerblockchain.mapper.BlockchainEvidenceMapper;
import com.gzu.volunteerblockchain.mapper.CompletionReviewMapper;
import com.gzu.volunteerblockchain.mapper.OrganizationMapper;
import com.gzu.volunteerblockchain.mapper.PointsRecordMapper;
import com.gzu.volunteerblockchain.mapper.UserMapper;
import com.gzu.volunteerblockchain.service.ActivityService;
import com.gzu.volunteerblockchain.service.BlockchainService;
import com.gzu.volunteerblockchain.service.PointBlockchainService;
import com.gzu.volunteerblockchain.vo.PlatformVOs;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ActivityServiceImpl implements ActivityService {

    private static final String REVIEW_DRAFT = "draft";
    private static final String REVIEW_PENDING = "pending_review";
    private static final String REVIEW_APPROVED = "approved";
    private static final String REVIEW_REJECTED = "rejected";
    private static final String COMPLETION_PENDING = "pending";
    private static final String COMPLETION_APPROVED = "approved";
    private static final String COMPLETION_REJECTED = "rejected";
    private static final String EVIDENCE_BIZ_COMPLETION = "completion";
    private static final String EVIDENCE_PENDING = "pending";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ActivityMapper activityMapper;
    private final ActivityParticipationMapper participationMapper;
    private final ActivityCompletionMapper completionMapper;
    private final CompletionReviewMapper completionReviewMapper;
    private final ActivityPublishReviewMapper publishReviewMapper;
    private final PointsRecordMapper pointsRecordMapper;
    private final BlockchainEvidenceMapper blockchainEvidenceMapper;
    private final OrganizationMapper organizationMapper;
    private final UserMapper userMapper;
    private final BlockchainService blockchainService;
    private final PointBlockchainService pointBlockchainService;
    private final ObjectMapper objectMapper;

    public ActivityServiceImpl(
        ActivityMapper activityMapper,
        ActivityParticipationMapper participationMapper,
        ActivityCompletionMapper completionMapper,
        CompletionReviewMapper completionReviewMapper,
        ActivityPublishReviewMapper publishReviewMapper,
        PointsRecordMapper pointsRecordMapper,
        BlockchainEvidenceMapper blockchainEvidenceMapper,
        OrganizationMapper organizationMapper,
        UserMapper userMapper,
        BlockchainService blockchainService,
        PointBlockchainService pointBlockchainService,
        ObjectMapper objectMapper
    ) {
        this.activityMapper = activityMapper;
        this.participationMapper = participationMapper;
        this.completionMapper = completionMapper;
        this.completionReviewMapper = completionReviewMapper;
        this.publishReviewMapper = publishReviewMapper;
        this.pointsRecordMapper = pointsRecordMapper;
        this.blockchainEvidenceMapper = blockchainEvidenceMapper;
        this.organizationMapper = organizationMapper;
        this.userMapper = userMapper;
        this.blockchainService = blockchainService;
        this.pointBlockchainService = pointBlockchainService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public PlatformVOs.ActivityVO createActivity(PlatformRequests.ActivitySaveRequest request) {
        AuthUser currentUser = requireOrganizationAdmin();
        validateActivitySaveRequest(request);

        Activity activity = new Activity();
        applyActivityRequest(activity, request);
        activity.setOrganizationId(currentUser.getOrganizationId());
        activity.setCreatedBy(currentUser.getUserId());
        activity.setCurrentParticipants(0);
        activity.setReviewStatus(REVIEW_DRAFT);
        activity.setStatus(resolveBusinessStatus(request.getStartDate(), request.getEndDate()));
        activity.setCreatedAt(LocalDateTime.now());
        activity.setUpdatedAt(LocalDateTime.now());
        activityMapper.insert(activity);
        return toActivityVO(activity, findOrganizationName(activity.getOrganizationId()), false, null);
    }

    @Override
    @Transactional
    public PlatformVOs.ActivityVO updateActivity(Integer id, PlatformRequests.ActivitySaveRequest request) {
        AuthUser currentUser = requireOrganizationAdmin();
        validateActivitySaveRequest(request);

        Activity activity = requireActivity(id);
        ensureSameOrganization(currentUser.getOrganizationId(), activity.getOrganizationId(), "不能修改其他组织的活动");
        if (activity.getCurrentParticipants() != null && activity.getCurrentParticipants() > 0) {
            throw new BusinessException("已有志愿者参与的活动不允许修改");
        }

        boolean reviewSensitiveChanged = hasReviewSensitiveChanges(activity, request);
        applyActivityRequest(activity, request);
        activity.setStatus(resolveBusinessStatus(request.getStartDate(), request.getEndDate()));
        if (REVIEW_APPROVED.equals(activity.getReviewStatus()) && reviewSensitiveChanged) {
            activity.setReviewStatus(REVIEW_PENDING);
            activity.setApprovedRewardPoints(null);
            activity.setReviewNote(null);
            activity.setReviewedBy(null);
            activity.setReviewedAt(null);
            activity.setPublishDate(null);
        }
        activity.setUpdatedAt(LocalDateTime.now());
        activityMapper.updateById(activity);
        return toActivityVO(activity, findOrganizationName(activity.getOrganizationId()), false, null);
    }

    @Override
    @Transactional
    public void deleteActivity(Integer id) {
        AuthUser currentUser = requireOrganizationAdmin();
        Activity activity = requireActivity(id);
        ensureSameOrganization(currentUser.getOrganizationId(), activity.getOrganizationId(), "不能删除其他组织的活动");
        if (activity.getCurrentParticipants() != null && activity.getCurrentParticipants() > 0) {
            throw new BusinessException("已有志愿者参与的活动不能删除");
        }
        Long completionCount = completionMapper.selectCount(new LambdaQueryWrapper<ActivityCompletion>()
            .eq(ActivityCompletion::getActivityId, id));
        if (completionCount != null && completionCount > 0) {
            throw new BusinessException("已产生完成报告的活动不能删除");
        }
        activityMapper.deleteById(id);
    }

    @Override
    @Transactional
    public PlatformVOs.ActivityVO submitActivity(Integer id) {
        AuthUser currentUser = requireOrganizationAdmin();
        Activity activity = requireActivity(id);
        ensureSameOrganization(currentUser.getOrganizationId(), activity.getOrganizationId(), "不能提交其他组织的活动");
        validateActivityForSubmit(activity);

        activity.setReviewStatus(REVIEW_PENDING);
        activity.setReviewNote(null);
        activity.setReviewedBy(null);
        activity.setReviewedAt(null);
        activity.setPublishDate(null);
        activity.setUpdatedAt(LocalDateTime.now());
        activityMapper.updateById(activity);

        ActivityPublishReview review = new ActivityPublishReview();
        review.setActivityId(activity.getActivityId());
        review.setReviewerId(currentUser.getUserId());
        review.setReviewerRole(RoleConstants.ORGANIZATION_ADMIN);
        review.setReviewAction("submitted");
        review.setRequestedRewardPoints(activity.getRequestedRewardPoints());
        review.setReviewNote("组织提交活动审核");
        review.setIsEscalated(Boolean.FALSE);
        review.setCreatedAt(LocalDateTime.now());
        publishReviewMapper.insert(review);

        return toActivityVO(activity, findOrganizationName(activity.getOrganizationId()), false, null);
    }

    @Override
    public List<PlatformVOs.ActivityVO> listVolunteerActivities() {
        AuthUser currentUser = requireVolunteer();
        List<Activity> activities = activityMapper.selectList(new LambdaQueryWrapper<Activity>()
            .eq(Activity::getReviewStatus, REVIEW_APPROVED)
            .orderByDesc(Activity::getPublishDate)
            .orderByDesc(Activity::getCreatedAt));

        Map<Integer, String> organizationNames = mapOrganizationNames(activities.stream().map(Activity::getOrganizationId).toList());
        Map<Integer, ActivityParticipation> participationMap = participationMapper.selectList(new LambdaQueryWrapper<ActivityParticipation>()
                .eq(ActivityParticipation::getUserId, currentUser.getUserId()))
            .stream()
            .collect(Collectors.toMap(ActivityParticipation::getActivityId, Function.identity(), (left, right) -> left));
        Map<Integer, String> completionStatusMap = completionMapper.selectList(new LambdaQueryWrapper<ActivityCompletion>()
                .eq(ActivityCompletion::getUserId, currentUser.getUserId()))
            .stream()
            .collect(Collectors.toMap(ActivityCompletion::getActivityId, ActivityCompletion::getCompletionStatus, (left, right) -> right));

        return activities.stream()
            .map(activity -> toActivityVO(
                activity,
                organizationNames.get(activity.getOrganizationId()),
                participationMap.containsKey(activity.getActivityId()),
                completionStatusMap.get(activity.getActivityId())
            ))
            .toList();
    }

    @Override
    public List<PlatformVOs.ActivityVO> listOrganizationActivities() {
        AuthUser currentUser = requireOrganizationAdmin();
        String organizationName = findOrganizationName(currentUser.getOrganizationId());
        return activityMapper.selectList(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getOrganizationId, currentUser.getOrganizationId())
                .orderByDesc(Activity::getCreatedAt))
            .stream()
            .map(activity -> toActivityVO(activity, organizationName, false, null))
            .toList();
    }

    @Override
    @Transactional
    public PlatformVOs.ParticipationVO joinActivity(Integer activityId) {
        AuthUser currentUser = requireVolunteer();
        Activity activity = requireActivity(activityId);
        ensureActivityJoinable(activity);

        Long exists = participationMapper.selectCount(new LambdaQueryWrapper<ActivityParticipation>()
            .eq(ActivityParticipation::getUserId, currentUser.getUserId())
            .eq(ActivityParticipation::getActivityId, activityId));
        if (exists != null && exists > 0) {
            throw new BusinessException("你已经报名该活动");
        }

        int updated = activityMapper.incrementCurrentParticipants(activityId);
        if (updated == 0) {
            throw new BusinessException("活动人数已满");
        }

        ActivityParticipation participation = new ActivityParticipation();
        participation.setUserId(currentUser.getUserId());
        participation.setActivityId(activityId);
        participation.setEarnedPoints(0);
        participation.setParticipationDate(LocalDateTime.now());
        participationMapper.insert(participation);

        return toParticipationVO(participation, activity, findOrganizationName(activity.getOrganizationId()), null);
    }

    @Override
    @Transactional
    public void cancelParticipation(Integer activityId) {
        AuthUser currentUser = requireVolunteer();
        ActivityParticipation participation = participationMapper.selectOne(new LambdaQueryWrapper<ActivityParticipation>()
            .eq(ActivityParticipation::getUserId, currentUser.getUserId())
            .eq(ActivityParticipation::getActivityId, activityId)
            .last("LIMIT 1"));
        if (participation == null) {
            throw new BusinessException("你尚未报名该活动");
        }

        Long completionCount = completionMapper.selectCount(new LambdaQueryWrapper<ActivityCompletion>()
            .eq(ActivityCompletion::getUserId, currentUser.getUserId())
            .eq(ActivityCompletion::getActivityId, activityId));
        if (completionCount != null && completionCount > 0) {
            throw new BusinessException("已提交完成报告的活动不能取消报名");
        }

        participationMapper.deleteById(participation.getParticipationId());
        activityMapper.decrementCurrentParticipants(activityId);
    }

    @Override
    public List<PlatformVOs.ParticipationVO> listMyParticipations() {
        AuthUser currentUser = requireVolunteer();
        List<ActivityParticipation> participations = participationMapper.selectList(new LambdaQueryWrapper<ActivityParticipation>()
            .eq(ActivityParticipation::getUserId, currentUser.getUserId())
            .orderByDesc(ActivityParticipation::getParticipationDate));
        Map<Integer, Activity> activityMap = mapActivities(participations.stream().map(ActivityParticipation::getActivityId).toList());
        Map<Integer, String> organizationNames = mapOrganizationNames(activityMap.values().stream().map(Activity::getOrganizationId).toList());
        Map<Integer, String> completionStatusMap = completionMapper.selectList(new LambdaQueryWrapper<ActivityCompletion>()
                .eq(ActivityCompletion::getUserId, currentUser.getUserId()))
            .stream()
            .collect(Collectors.toMap(ActivityCompletion::getActivityId, ActivityCompletion::getCompletionStatus, (left, right) -> right));

        return participations.stream()
            .map(item -> {
                Activity activity = activityMap.get(item.getActivityId());
                String organizationName = activity == null ? null : organizationNames.get(activity.getOrganizationId());
                return toParticipationVO(item, activity, organizationName, completionStatusMap.get(item.getActivityId()));
            })
            .toList();
    }

    @Override
    @Transactional
    public PlatformVOs.CompletionVO submitCompletion(PlatformRequests.CompletionSubmitRequest request) {
        AuthUser currentUser = requireVolunteer();
        Activity activity = requireActivity(request.getActivityId());
        ensureActivityVisible(activity);
        if (request.getServiceEndTime().isBefore(request.getServiceStartTime())) {
            throw new BusinessException("服务结束时间不能早于开始时间");
        }

        ActivityParticipation participation = participationMapper.selectOne(new LambdaQueryWrapper<ActivityParticipation>()
            .eq(ActivityParticipation::getUserId, currentUser.getUserId())
            .eq(ActivityParticipation::getActivityId, request.getActivityId())
            .last("LIMIT 1"));
        if (participation == null) {
            throw new BusinessException("请先报名活动，再提交完成报告");
        }

        ActivityCompletion latest = completionMapper.selectOne(new LambdaQueryWrapper<ActivityCompletion>()
            .eq(ActivityCompletion::getUserId, currentUser.getUserId())
            .eq(ActivityCompletion::getActivityId, request.getActivityId())
            .orderByDesc(ActivityCompletion::getSubmissionDate)
            .last("LIMIT 1"));
        if (latest != null && (COMPLETION_PENDING.equals(latest.getCompletionStatus()) || COMPLETION_APPROVED.equals(latest.getCompletionStatus()))) {
            throw new BusinessException("该活动已存在待审核或已通过的完成报告");
        }

        ActivityCompletion completion = new ActivityCompletion();
        completion.setUserId(currentUser.getUserId());
        completion.setActivityId(request.getActivityId());
        completion.setCompletionStatus(COMPLETION_PENDING);
        completion.setServiceLocation(trimRequired(request.getServiceLocation(), "服务地点不能为空"));
        completion.setServiceStartTime(request.getServiceStartTime());
        completion.setServiceEndTime(request.getServiceEndTime());
        completion.setReportText(trimRequired(request.getReportText(), "报告总结不能为空"));
        completion.setContributionDetails(trimRequired(request.getContributionDetails(), "贡献内容不能为空"));
        completion.setAttachmentPaths(writeAttachmentPaths(request.getAttachmentPaths()));
        completion.setSubmissionDate(LocalDateTime.now());
        completionMapper.insert(completion);

        return toCompletionVO(
            completion,
            activity,
            findOrganizationName(activity.getOrganizationId()),
            requireUser(currentUser.getUserId()),
            null
        );
    }

    @Override
    public List<PlatformVOs.CompletionVO> listMyCompletions() {
        AuthUser currentUser = requireVolunteer();
        List<ActivityCompletion> completions = completionMapper.selectList(new LambdaQueryWrapper<ActivityCompletion>()
            .eq(ActivityCompletion::getUserId, currentUser.getUserId())
            .orderByDesc(ActivityCompletion::getSubmissionDate));
        Map<Integer, Activity> activityMap = mapActivities(completions.stream().map(ActivityCompletion::getActivityId).toList());
        Map<Integer, String> organizationNames = mapOrganizationNames(activityMap.values().stream().map(Activity::getOrganizationId).toList());
        Map<Integer, BlockchainEvidence> evidenceMap = mapEvidencesByBizIds(completions.stream().map(ActivityCompletion::getCompletionId).toList());
        User volunteer = requireUser(currentUser.getUserId());

        return completions.stream()
            .map(item -> {
                Activity activity = activityMap.get(item.getActivityId());
                String organizationName = activity == null ? null : organizationNames.get(activity.getOrganizationId());
                return toCompletionVO(item, activity, organizationName, volunteer, evidenceMap.get(item.getCompletionId()));
            })
            .toList();
    }

    @Override
    public List<PlatformVOs.CompletionVO> listCompletionReviews(String status) {
        AuthUser currentUser = requireOrganizationAdmin();
        List<Activity> activities = activityMapper.selectList(new LambdaQueryWrapper<Activity>()
            .eq(Activity::getOrganizationId, currentUser.getOrganizationId()));
        if (activities.isEmpty()) {
            return List.of();
        }

        List<Integer> activityIds = activities.stream().map(Activity::getActivityId).toList();
        LambdaQueryWrapper<ActivityCompletion> query = new LambdaQueryWrapper<ActivityCompletion>()
            .in(ActivityCompletion::getActivityId, activityIds)
            .orderByDesc(ActivityCompletion::getSubmissionDate);
        if (status != null && !status.isBlank()) {
            query.eq(ActivityCompletion::getCompletionStatus, status.trim());
        }

        List<ActivityCompletion> completions = completionMapper.selectList(query);
        Map<Integer, Activity> activityMap = activities.stream()
            .collect(Collectors.toMap(Activity::getActivityId, Function.identity(), (left, right) -> left));
        Map<Integer, User> userMap = mapUsers(completions.stream().map(ActivityCompletion::getUserId).toList());
        Map<Integer, BlockchainEvidence> evidenceMap = mapEvidencesByBizIds(completions.stream().map(ActivityCompletion::getCompletionId).toList());
        String organizationName = findOrganizationName(currentUser.getOrganizationId());

        return completions.stream()
            .map(item -> toCompletionVO(item, activityMap.get(item.getActivityId()), organizationName, userMap.get(item.getUserId()), evidenceMap.get(item.getCompletionId())))
            .toList();
    }

    @Override
    @Transactional
    public PlatformVOs.CompletionVO reviewCompletion(Integer completionId, PlatformRequests.CompletionReviewRequest request) {
        AuthUser currentUser = requireOrganizationAdmin();
        ActivityCompletion completion = requireCompletion(completionId);
        Activity activity = requireActivity(completion.getActivityId());
        ensureSameOrganization(currentUser.getOrganizationId(), activity.getOrganizationId(), "不能审核其他组织活动的完成报告");
        if (!COMPLETION_PENDING.equals(completion.getCompletionStatus())) {
            throw new BusinessException("只有待审核的完成报告可以处理");
        }

        String status = normalizeStatus(request.getStatus());
        if (!COMPLETION_APPROVED.equals(status) && !COMPLETION_REJECTED.equals(status)) {
            throw new BusinessException("完成报告只能审核为 approved 或 rejected");
        }

        completion.setCompletionStatus(status);
        if (COMPLETION_APPROVED.equals(status)) {
            completion.setApprovedBy(currentUser.getUserId());
            completion.setApprovedAt(LocalDateTime.now());
            completion.setRejectReason(null);
        } else {
            completion.setApprovedBy(null);
            completion.setApprovedAt(null);
            completion.setRejectReason(trimToNull(request.getReviewNote()));
        }
        completionMapper.updateById(completion);

        CompletionReview review = new CompletionReview();
        review.setCompletionId(completionId);
        review.setReviewerId(currentUser.getUserId());
        review.setReviewAction(status);
        review.setReviewNote(trimToNull(request.getReviewNote()));
        review.setCreatedAt(LocalDateTime.now());
        completionReviewMapper.insert(review);

        BlockchainEvidence evidence = null;
        if (COMPLETION_APPROVED.equals(status)) {
            ActivityParticipation participation = participationMapper.selectOne(new LambdaQueryWrapper<ActivityParticipation>()
                .eq(ActivityParticipation::getUserId, completion.getUserId())
                .eq(ActivityParticipation::getActivityId, completion.getActivityId())
                .last("LIMIT 1"));
            if (participation == null) {
                throw new BusinessException("活动参与记录不存在，无法发放积分");
            }
            if (participation.getEarnedPoints() != null && participation.getEarnedPoints() > 0) {
                throw new BusinessException("该完成报告已发放过积分");
            }

            int points = activity.getApprovedRewardPoints() == null ? 0 : activity.getApprovedRewardPoints();
            participation.setEarnedPoints(points);
            participationMapper.updateById(participation);

            if (points > 0) {
                PointBlockchainService.PointChainResult pointResult = pointBlockchainService.creditPoints(
                    new PointBlockchainService.PointChainRequest(
                        "completion:" + completion.getCompletionId() + ":earn:" + completion.getUserId(),
                        completion.getUserId(),
                        activity.getOrganizationId(),
                        points,
                        "Activity completion approved",
                        EVIDENCE_BIZ_COMPLETION,
                        completion.getCompletionId()
                    )
                );
                userMapper.updateTotalPoints(completion.getUserId(), pointResult.chainBalanceAfter());
                PointsRecord record = new PointsRecord();
                record.setUserId(completion.getUserId());
                record.setOrganizationId(activity.getOrganizationId());
                record.setPoints(points);
                record.setTransactionType("earned");
                record.setSource("活动完成审核通过");
                record.setReferenceType(EVIDENCE_BIZ_COMPLETION);
                record.setReferenceId(completion.getCompletionId());
                record.setBizKey(pointResult.bizKey());
                record.setDigest(pointResult.digest());
                record.setTxHash(pointResult.txHash());
                record.setBlockNumber(pointResult.blockNumber());
                record.setContractName(pointResult.contractName());
                record.setOnchainStatus("success");
                record.setOnchainAt(pointResult.onchainAt());
                record.setChainBalanceAfter(pointResult.chainBalanceAfter());
                record.setCreatedAt(LocalDateTime.now());
                pointsRecordMapper.insert(record);
            }

            User volunteer = requireUser(completion.getUserId());
            evidence = new BlockchainEvidence();
            evidence.setBizType(EVIDENCE_BIZ_COMPLETION);
            evidence.setBizId(completion.getCompletionId());
            evidence.setDigest(blockchainService.buildDigest(completion, activity, volunteer));
            evidence.setContractName("VolunteerEvidenceRegistry");
            evidence.setOnchainStatus(EVIDENCE_PENDING);
            evidence.setStoragePath("/api/public/evidences/completion/" + completion.getCompletionId());
            evidence.setReviewerName(currentUser.getUsername());
            evidence.setReviewedAt(completion.getApprovedAt());
            evidence.setCreatedAt(LocalDateTime.now());
            blockchainEvidenceMapper.insert(evidence);

            Integer evidenceId = evidence.getId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    blockchainService.submitEvidenceAsync(evidenceId);
                }
            });
        }

        return toCompletionVO(completion, activity, findOrganizationName(activity.getOrganizationId()), requireUser(completion.getUserId()), evidence);
    }

    @Override
    public List<PlatformVOs.PointsRecordVO> listMyPointsRecords() {
        AuthUser currentUser = requireVolunteer();
        List<PointsRecord> records = pointsRecordMapper.selectList(new LambdaQueryWrapper<PointsRecord>()
            .eq(PointsRecord::getUserId, currentUser.getUserId())
            .orderByDesc(PointsRecord::getCreatedAt));
        Map<Integer, String> organizationNames = mapOrganizationNames(records.stream().map(PointsRecord::getOrganizationId).toList());
        return records.stream().map(record -> toPointsRecordVO(record, organizationNames.get(record.getOrganizationId()))).toList();
    }

    @Override
    public PlatformVOs.PointsBalanceVO getMyPointsBalance() {
        AuthUser currentUser = requireVolunteer();
        User user = requireUser(currentUser.getUserId());
        Integer chainBalance = pointBlockchainService.getBalance(currentUser.getUserId());
        Integer cachedBalance = user.getTotalPoints() == null ? 0 : user.getTotalPoints();
        PlatformVOs.PointsBalanceVO vo = new PlatformVOs.PointsBalanceVO();
        vo.setUserId(currentUser.getUserId());
        vo.setChainBalance(chainBalance);
        vo.setCachedBalance(cachedBalance);
        vo.setConsistent(Objects.equals(chainBalance, cachedBalance));
        vo.setCheckedAt(format(LocalDateTime.now()));
        return vo;
    }

    @Override
    public PlatformVOs.EvidenceVO getEvidence(String bizType, Integer bizId) {
        BlockchainEvidence evidence = blockchainEvidenceMapper.selectOne(new LambdaQueryWrapper<BlockchainEvidence>()
            .eq(BlockchainEvidence::getBizType, bizType)
            .eq(BlockchainEvidence::getBizId, bizId)
            .last("LIMIT 1"));
        if (evidence == null) {
            throw new BusinessException("链上存证记录不存在");
        }
        return toEvidenceVO(evidence);
    }

    @Override
    public List<PlatformVOs.EvidenceVO> listOrganizationEvidences() {
        AuthUser currentUser = requireOrganizationAdmin();
        List<Activity> activities = activityMapper.selectList(new LambdaQueryWrapper<Activity>()
            .eq(Activity::getOrganizationId, currentUser.getOrganizationId()));
        if (activities.isEmpty()) {
            return List.of();
        }

        List<Integer> activityIds = activities.stream().map(Activity::getActivityId).toList();
        List<ActivityCompletion> completions = completionMapper.selectList(new LambdaQueryWrapper<ActivityCompletion>()
            .in(ActivityCompletion::getActivityId, activityIds)
            .eq(ActivityCompletion::getCompletionStatus, COMPLETION_APPROVED));
        if (completions.isEmpty()) {
            return List.of();
        }

        return blockchainEvidenceMapper.selectList(new LambdaQueryWrapper<BlockchainEvidence>()
                .eq(BlockchainEvidence::getBizType, EVIDENCE_BIZ_COMPLETION)
                .in(BlockchainEvidence::getBizId, completions.stream().map(ActivityCompletion::getCompletionId).toList())
                .orderByDesc(BlockchainEvidence::getCreatedAt))
            .stream()
            .map(this::toEvidenceVO)
            .toList();
    }

    @Override
    public PlatformVOs.EvidenceVO retryEvidence(Integer id) {
        AuthUser currentUser = UserContext.getRequiredUser();
        BlockchainEvidence evidence = blockchainEvidenceMapper.selectById(id);
        if (evidence == null) {
            throw new BusinessException("链上存证记录不存在");
        }
        if (currentUser.hasRole(RoleConstants.ORGANIZATION_ADMIN) && !currentUser.hasRole(RoleConstants.SYSTEM_ADMIN)) {
            ActivityCompletion completion = requireCompletion(evidence.getBizId());
            Activity activity = requireActivity(completion.getActivityId());
            ensureSameOrganization(currentUser.getOrganizationId(), activity.getOrganizationId(), "不能重试其他组织的链上存证");
        }
        return toEvidenceVO(blockchainService.retryEvidence(id));
    }

    private void validateActivitySaveRequest(PlatformRequests.ActivitySaveRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException("活动结束时间不能早于开始时间");
        }
        if (request.getEnrollDeadline() != null && request.getEnrollDeadline().isAfter(request.getStartDate())) {
            throw new BusinessException("报名截止时间不能晚于活动开始时间");
        }
    }

    private void validateActivityForSubmit(Activity activity) {
        if (REVIEW_PENDING.equals(activity.getReviewStatus())) {
            throw new BusinessException("活动已在审核中");
        }
        if (REVIEW_APPROVED.equals(activity.getReviewStatus())) {
            throw new BusinessException("活动已审核通过，无需重复提交");
        }
        if (activity.getStartDate() == null || activity.getEndDate() == null || activity.getLocation() == null || activity.getActivityName() == null) {
            throw new BusinessException("活动信息不完整，不能提交审核");
        }
    }

    private void applyActivityRequest(Activity activity, PlatformRequests.ActivitySaveRequest request) {
        activity.setActivityName(trimRequired(request.getActivityName(), "活动名称不能为空"));
        activity.setDescription(trimToNull(request.getDescription()));
        activity.setStartDate(request.getStartDate());
        activity.setEndDate(request.getEndDate());
        activity.setLocation(trimRequired(request.getLocation(), "活动地点不能为空"));
        activity.setContactName(trimToNull(request.getContactName()));
        activity.setContactPhone(trimToNull(request.getContactPhone()));
        activity.setCategoryTags(trimToNull(request.getCategoryTags()));
        activity.setImagePath(trimToNull(request.getImagePath()));
        activity.setMaxParticipants(request.getMaxParticipants());
        activity.setRequestedRewardPoints(request.getRequestedRewardPoints());
        activity.setEnrollDeadline(request.getEnrollDeadline());
    }

    private boolean hasReviewSensitiveChanges(Activity activity, PlatformRequests.ActivitySaveRequest request) {
        return !Objects.equals(activity.getStartDate(), request.getStartDate())
            || !Objects.equals(activity.getEndDate(), request.getEndDate())
            || !Objects.equals(trimToNull(activity.getLocation()), trimToNull(request.getLocation()))
            || !Objects.equals(trimToNull(activity.getCategoryTags()), trimToNull(request.getCategoryTags()))
            || !Objects.equals(activity.getMaxParticipants(), request.getMaxParticipants())
            || !Objects.equals(activity.getRequestedRewardPoints(), request.getRequestedRewardPoints());
    }

    private void ensureActivityJoinable(Activity activity) {
        ensureActivityVisible(activity);
        if ("completed".equals(resolveBusinessStatus(activity.getStartDate(), activity.getEndDate()))) {
            throw new BusinessException("已结束活动不能报名");
        }
        if (activity.getEnrollDeadline() != null && activity.getEnrollDeadline().isBefore(LocalDateTime.now())) {
            throw new BusinessException("报名截止时间已过");
        }
    }

    private void ensureActivityVisible(Activity activity) {
        if (!REVIEW_APPROVED.equals(activity.getReviewStatus())) {
            throw new BusinessException("该活动尚未通过平台审核");
        }
    }

    private PlatformVOs.ActivityVO toActivityVO(Activity activity, String organizationName, boolean joined, String completionStatus) {
        PlatformVOs.ActivityVO vo = new PlatformVOs.ActivityVO();
        vo.setActivityId(activity.getActivityId());
        vo.setOrganizationId(activity.getOrganizationId());
        vo.setOrganizationName(organizationName);
        vo.setActivityName(activity.getActivityName());
        vo.setDescription(activity.getDescription());
        vo.setStartDate(format(activity.getStartDate()));
        vo.setEndDate(format(activity.getEndDate()));
        vo.setPublishDate(format(activity.getPublishDate()));
        vo.setLocation(activity.getLocation());
        vo.setContactName(activity.getContactName());
        vo.setContactPhone(activity.getContactPhone());
        vo.setCategoryTags(activity.getCategoryTags());
        vo.setImagePath(activity.getImagePath());
        vo.setMaxParticipants(activity.getMaxParticipants());
        vo.setCurrentParticipants(activity.getCurrentParticipants());
        vo.setRequestedRewardPoints(activity.getRequestedRewardPoints());
        vo.setApprovedRewardPoints(activity.getApprovedRewardPoints());
        vo.setEnrollDeadline(format(activity.getEnrollDeadline()));
        vo.setReviewStatus(activity.getReviewStatus());
        vo.setReviewNote(activity.getReviewNote());
        vo.setStatus(resolveBusinessStatus(activity.getStartDate(), activity.getEndDate()));
        vo.setJoined(joined);
        vo.setCompletionStatus(completionStatus);
        return vo;
    }

    private PlatformVOs.ParticipationVO toParticipationVO(ActivityParticipation participation, Activity activity, String organizationName, String completionStatus) {
        PlatformVOs.ParticipationVO vo = new PlatformVOs.ParticipationVO();
        vo.setParticipationId(participation.getParticipationId());
        vo.setActivityId(participation.getActivityId());
        vo.setOrganizationId(activity == null ? null : activity.getOrganizationId());
        vo.setOrganizationName(organizationName);
        vo.setActivityName(activity == null ? "未知活动" : activity.getActivityName());
        vo.setLocation(activity == null ? null : activity.getLocation());
        vo.setParticipationDate(format(participation.getParticipationDate()));
        vo.setEarnedPoints(participation.getEarnedPoints());
        vo.setCompletionStatus(completionStatus);
        return vo;
    }

    private PlatformVOs.CompletionVO toCompletionVO(ActivityCompletion completion, Activity activity, String organizationName, User volunteer, BlockchainEvidence evidence) {
        PlatformVOs.CompletionVO vo = new PlatformVOs.CompletionVO();
        vo.setCompletionId(completion.getCompletionId());
        vo.setActivityId(completion.getActivityId());
        vo.setActivityName(activity == null ? "未知活动" : activity.getActivityName());
        vo.setOrganizationId(activity == null ? null : activity.getOrganizationId());
        vo.setOrganizationName(organizationName);
        vo.setUserId(completion.getUserId());
        vo.setUsername(volunteer == null ? "未知用户" : volunteer.getUsername());
        vo.setStatus(completion.getCompletionStatus());
        vo.setReportText(completion.getReportText());
        vo.setContributionDetails(completion.getContributionDetails());
        vo.setServiceLocation(completion.getServiceLocation());
        vo.setServiceStartTime(format(completion.getServiceStartTime()));
        vo.setServiceEndTime(format(completion.getServiceEndTime()));
        vo.setAttachmentPaths(readAttachmentPaths(completion.getAttachmentPaths()));
        vo.setRejectReason(completion.getRejectReason());
        vo.setApprovedAt(format(completion.getApprovedAt()));
        vo.setSubmissionDate(format(completion.getSubmissionDate()));
        vo.setEvidenceId(evidence == null ? null : evidence.getId());
        vo.setEvidenceStatus(evidence == null ? null : evidence.getOnchainStatus());
        vo.setTxHash(evidence == null ? null : evidence.getTxHash());
        vo.setDigest(evidence == null ? null : evidence.getDigest());
        return vo;
    }

    private PlatformVOs.PointsRecordVO toPointsRecordVO(PointsRecord record, String organizationName) {
        PlatformVOs.PointsRecordVO vo = new PlatformVOs.PointsRecordVO();
        vo.setRecordId(record.getRecordId());
        vo.setOrganizationId(record.getOrganizationId());
        vo.setOrganizationName(organizationName);
        vo.setPoints(record.getPoints());
        vo.setTransactionType(record.getTransactionType());
        vo.setSource(record.getSource());
        vo.setReferenceType(record.getReferenceType());
        vo.setReferenceId(record.getReferenceId());
        vo.setBizKey(record.getBizKey());
        vo.setDigest(record.getDigest());
        vo.setTxHash(record.getTxHash());
        vo.setBlockNumber(record.getBlockNumber());
        vo.setContractName(record.getContractName());
        vo.setOnchainStatus(record.getOnchainStatus());
        vo.setErrorMessage(record.getErrorMessage());
        vo.setOnchainAt(format(record.getOnchainAt()));
        vo.setChainBalanceAfter(record.getChainBalanceAfter());
        vo.setCreatedAt(format(record.getCreatedAt()));
        return vo;
    }

    private PlatformVOs.EvidenceVO toEvidenceVO(BlockchainEvidence evidence) {
        PlatformVOs.EvidenceVO vo = new PlatformVOs.EvidenceVO();
        vo.setId(evidence.getId());
        vo.setBizType(evidence.getBizType());
        vo.setBizId(evidence.getBizId());
        vo.setDigest(evidence.getDigest());
        vo.setTxHash(evidence.getTxHash());
        vo.setBlockNumber(evidence.getBlockNumber());
        vo.setContractName(evidence.getContractName());
        vo.setOnchainStatus(evidence.getOnchainStatus());
        vo.setErrorMessage(evidence.getErrorMessage());
        vo.setReviewerName(evidence.getReviewerName());
        vo.setReviewedAt(format(evidence.getReviewedAt()));
        vo.setOnchainAt(format(evidence.getOnchainAt()));
        vo.setStoragePath(evidence.getStoragePath());

        if (EVIDENCE_BIZ_COMPLETION.equals(evidence.getBizType())) {
            ActivityCompletion completion = completionMapper.selectById(evidence.getBizId());
            if (completion != null) {
                Activity activity = activityMapper.selectById(completion.getActivityId());
                User volunteer = userMapper.selectById(completion.getUserId());
                Organization organization = activity == null ? null : organizationMapper.selectById(activity.getOrganizationId());
                vo.setActivityId(activity == null ? null : activity.getActivityId());
                vo.setActivityName(activity == null ? null : activity.getActivityName());
                vo.setOrganizationId(activity == null ? null : activity.getOrganizationId());
                vo.setOrganizationName(organization == null ? null : organization.getOrganizationName());
                vo.setUserId(completion.getUserId());
                vo.setUsername(volunteer == null ? null : volunteer.getUsername());
                vo.setVerified(verifyEvidence(evidence, completion, activity, volunteer));
            } else {
                vo.setVerified(Boolean.FALSE);
            }
        } else {
            vo.setVerified(Boolean.FALSE);
        }
        return vo;
    }

    private Map<Integer, Activity> mapActivities(List<Integer> activityIds) {
        if (activityIds == null || activityIds.isEmpty()) {
            return Map.of();
        }
        return activityMapper.selectList(new LambdaQueryWrapper<Activity>().in(Activity::getActivityId, activityIds))
            .stream()
            .collect(Collectors.toMap(Activity::getActivityId, Function.identity(), (left, right) -> left));
    }

    private Map<Integer, User> mapUsers(List<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getUserId, userIds))
            .stream()
            .collect(Collectors.toMap(User::getUserId, Function.identity(), (left, right) -> left));
    }

    private Map<Integer, String> mapOrganizationNames(List<Integer> organizationIds) {
        if (organizationIds == null || organizationIds.isEmpty()) {
            return Map.of();
        }
        return organizationMapper.selectBatchIds(organizationIds.stream().filter(Objects::nonNull).distinct().toList()).stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Organization::getOrganizationId, Organization::getOrganizationName, (left, right) -> left));
    }

    private Map<Integer, BlockchainEvidence> mapEvidencesByBizIds(List<Integer> bizIds) {
        if (bizIds == null || bizIds.isEmpty()) {
            return Map.of();
        }
        return blockchainEvidenceMapper.selectList(new LambdaQueryWrapper<BlockchainEvidence>()
                .eq(BlockchainEvidence::getBizType, EVIDENCE_BIZ_COMPLETION)
                .in(BlockchainEvidence::getBizId, bizIds))
            .stream()
            .collect(Collectors.toMap(BlockchainEvidence::getBizId, Function.identity(), (left, right) -> left));
    }

    private Activity requireActivity(Integer id) {
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        return activity;
    }

    private ActivityCompletion requireCompletion(Integer id) {
        ActivityCompletion completion = completionMapper.selectById(id);
        if (completion == null) {
            throw new BusinessException("完成报告不存在");
        }
        return completion;
    }

    private User requireUser(Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private AuthUser requireVolunteer() {
        AuthUser currentUser = UserContext.getRequiredUser();
        if (!currentUser.hasRole(RoleConstants.VOLUNTEER)) {
            throw new BusinessException("当前接口仅志愿者可访问");
        }
        return currentUser;
    }

    private AuthUser requireOrganizationAdmin() {
        AuthUser currentUser = UserContext.getRequiredUser();
        if (!currentUser.hasRole(RoleConstants.ORGANIZATION_ADMIN)) {
            throw new BusinessException("当前接口仅组织管理员可访问");
        }
        if (currentUser.getOrganizationId() == null) {
            throw new BusinessException("当前组织管理员未绑定组织");
        }
        return currentUser;
    }

    private void ensureSameOrganization(Integer expectedOrgId, Integer actualOrgId, String message) {
        if (expectedOrgId == null || actualOrgId == null || !expectedOrgId.equals(actualOrgId)) {
            throw new BusinessException(message);
        }
    }

    private String findOrganizationName(Integer organizationId) {
        if (organizationId == null) {
            return null;
        }
        Organization organization = organizationMapper.selectById(organizationId);
        return organization == null ? null : organization.getOrganizationName();
    }

    private String resolveBusinessStatus(LocalDateTime startDate, LocalDateTime endDate) {
        LocalDateTime now = LocalDateTime.now();
        if (endDate != null && endDate.isBefore(now)) {
            return "completed";
        }
        if (startDate != null && startDate.isAfter(now)) {
            return "pending";
        }
        return "ongoing";
    }

    private String trimRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toLowerCase();
    }

    private String format(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_FORMATTER);
    }

    private String writeAttachmentPaths(List<String> attachmentPaths) {
        try {
            return objectMapper.writeValueAsString(attachmentPaths == null ? List.of() : attachmentPaths);
        } catch (Exception ex) {
            throw new BusinessException("附件信息序列化失败: " + ex.getMessage());
        }
    }

    private List<String> readAttachmentPaths(String raw) {
        try {
            if (raw == null || raw.isBlank()) {
                return List.of();
            }
            return objectMapper.readValue(raw, new TypeReference<List<String>>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    private boolean verifyEvidence(BlockchainEvidence evidence, ActivityCompletion completion, Activity activity, User volunteer) {
        if (evidence == null || completion == null || activity == null || volunteer == null) {
            return false;
        }
        if (!COMPLETION_APPROVED.equals(completion.getCompletionStatus())) {
            return false;
        }
        return Objects.equals(evidence.getDigest(), blockchainService.buildDigest(completion, activity, volunteer));
    }
}
