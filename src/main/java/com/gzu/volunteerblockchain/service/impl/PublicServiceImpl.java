package com.gzu.volunteerblockchain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gzu.volunteerblockchain.entity.Activity;
import com.gzu.volunteerblockchain.entity.ActivityCompletion;
import com.gzu.volunteerblockchain.entity.BlockchainEvidence;
import com.gzu.volunteerblockchain.entity.Organization;
import com.gzu.volunteerblockchain.entity.User;
import com.gzu.volunteerblockchain.exception.BusinessException;
import com.gzu.volunteerblockchain.mapper.ActivityCompletionMapper;
import com.gzu.volunteerblockchain.mapper.ActivityMapper;
import com.gzu.volunteerblockchain.mapper.BlockchainEvidenceMapper;
import com.gzu.volunteerblockchain.mapper.OrganizationMapper;
import com.gzu.volunteerblockchain.mapper.UserMapper;
import com.gzu.volunteerblockchain.service.BlockchainService;
import com.gzu.volunteerblockchain.service.PublicService;
import com.gzu.volunteerblockchain.vo.InfoItemVO;
import com.gzu.volunteerblockchain.vo.PlatformVOs;
import com.gzu.volunteerblockchain.vo.ProjectItemVO;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class PublicServiceImpl implements PublicService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ActivityMapper activityMapper;
    private final ActivityCompletionMapper completionMapper;
    private final BlockchainEvidenceMapper blockchainEvidenceMapper;
    private final OrganizationMapper organizationMapper;
    private final UserMapper userMapper;
    private final BlockchainService blockchainService;

    public PublicServiceImpl(
        ActivityMapper activityMapper,
        ActivityCompletionMapper completionMapper,
        BlockchainEvidenceMapper blockchainEvidenceMapper,
        OrganizationMapper organizationMapper,
        UserMapper userMapper,
        BlockchainService blockchainService
    ) {
        this.activityMapper = activityMapper;
        this.completionMapper = completionMapper;
        this.blockchainEvidenceMapper = blockchainEvidenceMapper;
        this.organizationMapper = organizationMapper;
        this.userMapper = userMapper;
        this.blockchainService = blockchainService;
    }

    @Override
    public List<InfoItemVO> getInfoList() {
        return List.of(
            new InfoItemVO("平台启用活动积分审核机制", "公告", "2026-04-01"),
            new InfoItemVO("统一积分商城正式上线", "公告", "2026-03-28"),
            new InfoItemVO("公开链上存证查询已开放", "通知", "2026-03-25"),
            new InfoItemVO("高积分活动将进入超级管理员复审", "规则", "2026-03-20")
        );
    }

    @Override
    public List<ProjectItemVO> getCompletedProjects(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        List<ActivityCompletion> completions = completionMapper.selectList(new LambdaQueryWrapper<ActivityCompletion>()
            .eq(ActivityCompletion::getCompletionStatus, "approved")
            .orderByDesc(ActivityCompletion::getApprovedAt)
            .last("LIMIT " + safeLimit));
        if (completions.isEmpty()) {
            return List.of();
        }

        Map<Integer, Activity> activityMap = mapActivities(completions.stream().map(ActivityCompletion::getActivityId).toList());
        Map<Integer, User> userMap = mapUsers(completions.stream().map(ActivityCompletion::getUserId).toList());
        Map<Integer, BlockchainEvidence> evidenceMap = mapEvidencesByBizIds(completions.stream().map(ActivityCompletion::getCompletionId).toList());

        List<ProjectItemVO> result = new ArrayList<>();
        for (ActivityCompletion completion : completions) {
            Activity activity = activityMap.get(completion.getActivityId());
            if (activity == null) {
                continue;
            }
            User volunteer = userMap.get(completion.getUserId());
            User reviewer = completion.getApprovedBy() == null ? null : userMapper.selectById(completion.getApprovedBy());
            Organization organization = activity.getOrganizationId() == null ? null : organizationMapper.selectById(activity.getOrganizationId());
            BlockchainEvidence evidence = evidenceMap.get(completion.getCompletionId());

            ProjectItemVO vo = new ProjectItemVO();
            vo.setId(completion.getCompletionId());
            vo.setTitle(activity.getActivityName());
            vo.setDescription(completion.getReportText() == null ? activity.getDescription() : completion.getReportText());
            vo.setLocation(activity.getLocation());
            vo.setEndDate(activity.getEndDate() == null ? null : activity.getEndDate().format(DATE_FORMATTER));
            vo.setImage(activity.getImagePath());
            vo.setOrganizationName(organization == null ? null : organization.getOrganizationName());
            vo.setVolunteerName(volunteer == null ? null : volunteer.getUsername());
            vo.setReviewerName(reviewer == null ? null : reviewer.getUsername());
            vo.setReviewTime(completion.getApprovedAt() == null ? null : completion.getApprovedAt().format(DATE_TIME_FORMATTER));
            vo.setEvidenceStatus(evidence == null ? "pending" : evidence.getOnchainStatus());
            vo.setTxHash(evidence == null ? null : evidence.getTxHash());
            vo.setDigest(evidence == null ? null : evidence.getDigest());
            vo.setVerified(evidence != null && volunteer != null && evidence.getDigest() != null
                && evidence.getDigest().equals(blockchainService.buildDigest(completion, activity, volunteer)));
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<PlatformVOs.EvidenceVO> listPublicEvidences(Integer limit, String status, Integer organizationId, String keyword) {
        int safeLimit = limit == null ? 20 : Math.max(1, Math.min(limit, 100));
        LambdaQueryWrapper<BlockchainEvidence> query = new LambdaQueryWrapper<BlockchainEvidence>()
            .eq(BlockchainEvidence::getBizType, "completion")
            .orderByDesc(BlockchainEvidence::getCreatedAt)
            .last("LIMIT " + Math.max(50, safeLimit * 3));
        if (status != null && !status.isBlank()) {
            query.eq(BlockchainEvidence::getOnchainStatus, status.trim());
        }

        List<BlockchainEvidence> evidences = blockchainEvidenceMapper.selectList(query);
        if (evidences.isEmpty()) {
            return List.of();
        }

        Map<Integer, ActivityCompletion> completionMap = completionMapper.selectList(new LambdaQueryWrapper<ActivityCompletion>()
                .in(ActivityCompletion::getCompletionId, evidences.stream().map(BlockchainEvidence::getBizId).toList()))
            .stream()
            .collect(Collectors.toMap(ActivityCompletion::getCompletionId, Function.identity(), (left, right) -> left));
        Map<Integer, Activity> activityMap = mapActivities(completionMap.values().stream().map(ActivityCompletion::getActivityId).toList());
        Map<Integer, User> userMap = mapUsers(completionMap.values().stream().map(ActivityCompletion::getUserId).toList());
        Map<Integer, Organization> organizationMap = mapOrganizations(activityMap.values().stream().map(Activity::getOrganizationId).toList());

        return evidences.stream()
            .map(evidence -> toEvidenceVO(evidence, completionMap.get(evidence.getBizId()), activityMap, userMap, organizationMap))
            .filter(Objects::nonNull)
            .filter(vo -> organizationId == null || Objects.equals(vo.getOrganizationId(), organizationId))
            .filter(vo -> matchKeyword(vo, keyword))
            .limit(safeLimit)
            .toList();
    }

    @Override
    public PlatformVOs.EvidenceVO getPublicEvidence(String bizType, Integer bizId) {
        BlockchainEvidence evidence = blockchainEvidenceMapper.selectOne(new LambdaQueryWrapper<BlockchainEvidence>()
            .eq(BlockchainEvidence::getBizType, bizType)
            .eq(BlockchainEvidence::getBizId, bizId)
            .last("LIMIT 1"));
        if (evidence == null) {
            throw new BusinessException("链上存证记录不存在");
        }

        ActivityCompletion completion = completionMapper.selectById(evidence.getBizId());
        Map<Integer, Activity> activityMap = completion == null ? Map.of() : mapActivities(List.of(completion.getActivityId()));
        Map<Integer, User> userMap = completion == null ? Map.of() : mapUsers(List.of(completion.getUserId()));
        Map<Integer, Organization> organizationMap = activityMap.isEmpty() ? Map.of() : mapOrganizations(activityMap.values().stream().map(Activity::getOrganizationId).toList());
        PlatformVOs.EvidenceVO vo = toEvidenceVO(evidence, completion, activityMap, userMap, organizationMap);
        if (vo == null) {
            throw new BusinessException("链上存证记录不存在");
        }
        return vo;
    }

    private PlatformVOs.EvidenceVO toEvidenceVO(
        BlockchainEvidence evidence,
        ActivityCompletion completion,
        Map<Integer, Activity> activityMap,
        Map<Integer, User> userMap,
        Map<Integer, Organization> organizationMap
    ) {
        if (completion == null) {
            return null;
        }
        Activity activity = activityMap.get(completion.getActivityId());
        User volunteer = userMap.get(completion.getUserId());
        Organization organization = activity == null ? null : organizationMap.get(activity.getOrganizationId());

        PlatformVOs.EvidenceVO vo = new PlatformVOs.EvidenceVO();
        vo.setId(evidence.getId());
        vo.setBizType(evidence.getBizType());
        vo.setBizId(evidence.getBizId());
        vo.setActivityId(activity == null ? null : activity.getActivityId());
        vo.setActivityName(activity == null ? null : activity.getActivityName());
        vo.setOrganizationId(activity == null ? null : activity.getOrganizationId());
        vo.setOrganizationName(organization == null ? null : organization.getOrganizationName());
        vo.setUserId(completion.getUserId());
        vo.setUsername(volunteer == null ? null : volunteer.getUsername());
        vo.setDigest(evidence.getDigest());
        vo.setTxHash(evidence.getTxHash());
        vo.setBlockNumber(evidence.getBlockNumber());
        vo.setContractName(evidence.getContractName());
        vo.setOnchainStatus(evidence.getOnchainStatus());
        vo.setErrorMessage(evidence.getErrorMessage());
        vo.setReviewerName(evidence.getReviewerName());
        vo.setReviewedAt(evidence.getReviewedAt() == null ? null : evidence.getReviewedAt().format(DATE_TIME_FORMATTER));
        vo.setOnchainAt(evidence.getOnchainAt() == null ? null : evidence.getOnchainAt().format(DATE_TIME_FORMATTER));
        vo.setStoragePath(evidence.getStoragePath());
        vo.setVerified(activity != null && volunteer != null && evidence.getDigest() != null
            && evidence.getDigest().equals(blockchainService.buildDigest(completion, activity, volunteer)));
        return vo;
    }

    private boolean matchKeyword(PlatformVOs.EvidenceVO vo, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String needle = keyword.trim().toLowerCase();
        return contains(vo.getActivityName(), needle)
            || contains(vo.getOrganizationName(), needle)
            || contains(vo.getUsername(), needle)
            || contains(vo.getDigest(), needle);
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }

    private Map<Integer, Activity> mapActivities(List<Integer> activityIds) {
        if (activityIds == null || activityIds.isEmpty()) {
            return Map.of();
        }
        return activityMapper.selectList(new LambdaQueryWrapper<Activity>().in(Activity::getActivityId, activityIds)).stream()
            .collect(LinkedHashMap::new, (map, item) -> map.put(item.getActivityId(), item), Map::putAll);
    }

    private Map<Integer, User> mapUsers(List<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getUserId, userIds)).stream()
            .collect(LinkedHashMap::new, (map, item) -> map.put(item.getUserId(), item), Map::putAll);
    }

    private Map<Integer, Organization> mapOrganizations(List<Integer> organizationIds) {
        if (organizationIds == null || organizationIds.isEmpty()) {
            return Map.of();
        }
        return organizationMapper.selectBatchIds(organizationIds.stream().filter(Objects::nonNull).distinct().toList()).stream()
            .filter(Objects::nonNull)
            .collect(LinkedHashMap::new, (map, item) -> map.put(item.getOrganizationId(), item), Map::putAll);
    }

    private Map<Integer, BlockchainEvidence> mapEvidencesByBizIds(List<Integer> bizIds) {
        if (bizIds == null || bizIds.isEmpty()) {
            return Map.of();
        }
        return blockchainEvidenceMapper.selectList(new LambdaQueryWrapper<BlockchainEvidence>()
                .eq(BlockchainEvidence::getBizType, "completion")
                .in(BlockchainEvidence::getBizId, bizIds))
            .stream()
            .collect(LinkedHashMap::new, (map, item) -> map.put(item.getBizId(), item), Map::putAll);
    }
}
