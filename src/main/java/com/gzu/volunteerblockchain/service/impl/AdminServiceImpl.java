package com.gzu.volunteerblockchain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gzu.volunteerblockchain.common.AuthUser;
import com.gzu.volunteerblockchain.common.RoleConstants;
import com.gzu.volunteerblockchain.common.UserContext;
import com.gzu.volunteerblockchain.dto.AdminRequests;
import com.gzu.volunteerblockchain.entity.Activity;
import com.gzu.volunteerblockchain.entity.ActivityPublishReview;
import com.gzu.volunteerblockchain.entity.Organization;
import com.gzu.volunteerblockchain.entity.Permission;
import com.gzu.volunteerblockchain.entity.PointsRecord;
import com.gzu.volunteerblockchain.entity.PointsRuleConfig;
import com.gzu.volunteerblockchain.entity.Product;
import com.gzu.volunteerblockchain.entity.ProductReview;
import com.gzu.volunteerblockchain.entity.Role;
import com.gzu.volunteerblockchain.entity.User;
import com.gzu.volunteerblockchain.exception.BusinessException;
import com.gzu.volunteerblockchain.mapper.ActivityMapper;
import com.gzu.volunteerblockchain.mapper.ActivityPublishReviewMapper;
import com.gzu.volunteerblockchain.mapper.OrganizationMapper;
import com.gzu.volunteerblockchain.mapper.PointsRecordMapper;
import com.gzu.volunteerblockchain.mapper.PointsRuleConfigMapper;
import com.gzu.volunteerblockchain.mapper.ProductMapper;
import com.gzu.volunteerblockchain.mapper.ProductReviewMapper;
import com.gzu.volunteerblockchain.mapper.UserMapper;
import com.gzu.volunteerblockchain.service.AdminService;
import com.gzu.volunteerblockchain.service.PointBlockchainService;
import com.gzu.volunteerblockchain.service.RbacService;
import com.gzu.volunteerblockchain.vo.AdminVOs;
import com.gzu.volunteerblockchain.vo.PlatformVOs;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminServiceImpl implements AdminService {

    private static final String REVIEW_PENDING = "pending_review";
    private static final String REVIEW_APPROVED = "approved";
    private static final String REVIEW_REJECTED = "rejected";
    private static final String REVIEW_ESCALATED = "escalated";
    private static final String REVIEW_DRAFT = "draft";
    private static final String PRODUCT_OFF_SHELF = "off_shelf";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserMapper userMapper;
    private final OrganizationMapper organizationMapper;
    private final ActivityMapper activityMapper;
    private final ActivityPublishReviewMapper activityPublishReviewMapper;
    private final ProductMapper productMapper;
    private final ProductReviewMapper productReviewMapper;
    private final PointsRecordMapper pointsRecordMapper;
    private final PointsRuleConfigMapper pointsRuleConfigMapper;
    private final RbacService rbacService;
    private final PointBlockchainService pointBlockchainService;

    public AdminServiceImpl(
        UserMapper userMapper,
        OrganizationMapper organizationMapper,
        ActivityMapper activityMapper,
        ActivityPublishReviewMapper activityPublishReviewMapper,
        ProductMapper productMapper,
        ProductReviewMapper productReviewMapper,
        PointsRecordMapper pointsRecordMapper,
        PointsRuleConfigMapper pointsRuleConfigMapper,
        RbacService rbacService,
        PointBlockchainService pointBlockchainService
    ) {
        this.userMapper = userMapper;
        this.organizationMapper = organizationMapper;
        this.activityMapper = activityMapper;
        this.activityPublishReviewMapper = activityPublishReviewMapper;
        this.productMapper = productMapper;
        this.productReviewMapper = productReviewMapper;
        this.pointsRecordMapper = pointsRecordMapper;
        this.pointsRuleConfigMapper = pointsRuleConfigMapper;
        this.rbacService = rbacService;
        this.pointBlockchainService = pointBlockchainService;
    }

    @Override
    public List<AdminVOs.AdminUserVO> listUsers() {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().orderByAsc(User::getUserId));
        Map<Integer, String> organizationNames = mapOrganizationNames(users);
        return users.stream().map(user -> toAdminUserVO(user, organizationNames)).toList();
    }

    @Override
    public AdminVOs.UserRolesVO getUserRoles(Integer userId) {
        User user = requireUser(userId);
        AuthUser authUser = rbacService.buildAuthUser(user);
        AdminVOs.UserRolesVO vo = new AdminVOs.UserRolesVO();
        vo.setUserId(user.getUserId());
        vo.setPrimaryRole(authUser.getPrimaryRole());
        vo.setRoles(new ArrayList<>(authUser.getRoles()));
        return vo;
    }

    @Override
    @Transactional
    public AdminVOs.UserRolesVO assignUserRoles(Integer userId, AdminRequests.AssignUserRolesRequest request) {
        requireUser(userId);
        rbacService.replaceUserRoles(userId, request.getRoleCodes());
        return getUserRoles(userId);
    }

    @Override
    public List<AdminVOs.RoleVO> listRoles() {
        return rbacService.listRoles().stream().map(this::toRoleVO).toList();
    }

    @Override
    public AdminVOs.RolePermissionsVO getRolePermissions(String roleCode) {
        AdminVOs.RolePermissionsVO vo = new AdminVOs.RolePermissionsVO();
        vo.setRoleCode(roleCode);
        vo.setPermissions(rbacService.getRolePermissionCodes(roleCode));
        return vo;
    }

    @Override
    public List<AdminVOs.PermissionVO> listPermissions() {
        return rbacService.listPermissions().stream().map(this::toPermissionVO).toList();
    }

    @Override
    public List<AdminVOs.ActivityReviewVO> listActivityReviews(String status) {
        requireActivityReviewer();
        LambdaQueryWrapper<Activity> query = new LambdaQueryWrapper<Activity>().orderByDesc(Activity::getUpdatedAt);
        if (status == null || status.isBlank()) {
            query.eq(Activity::getReviewStatus, REVIEW_PENDING);
        } else {
            query.eq(Activity::getReviewStatus, status.trim());
        }

        List<Activity> activities = activityMapper.selectList(query);
        Map<Integer, String> organizationNames = organizationMapper.selectBatchIds(
            activities.stream().map(Activity::getOrganizationId).filter(Objects::nonNull).distinct().toList()
        ).stream().filter(Objects::nonNull)
            .collect(LinkedHashMap::new, (map, organization) -> map.put(organization.getOrganizationId(), organization.getOrganizationName()), Map::putAll);
        Map<Integer, User> userMap = userMapper.selectList(new LambdaQueryWrapper<User>()
                .in(!activities.isEmpty(), User::getUserId, activities.stream().map(Activity::getCreatedBy).filter(Objects::nonNull).distinct().toList()))
            .stream()
            .collect(LinkedHashMap::new, (map, user) -> map.put(user.getUserId(), user), Map::putAll);

        return activities.stream().map(activity -> toActivityReviewVO(
            activity,
            organizationNames.get(activity.getOrganizationId()),
            userMap.get(activity.getCreatedBy()),
            matchRule(activity)
        )).toList();
    }

    @Override
    @Transactional
    public PlatformVOs.ActivityVO reviewActivity(Integer activityId, AdminRequests.ActivityReviewRequest request) {
        AuthUser currentUser = requireActivityReviewer();
        Activity activity = requireActivity(activityId);
        if (!REVIEW_PENDING.equals(activity.getReviewStatus())) {
            throw new BusinessException("只有待审核活动才能处理");
        }
        ensureNotSelfReview(currentUser, activity.getOrganizationId(), "不能审核自己所属组织提交的活动");

        String status = normalizeStatus(request.getStatus());
        RuleMatch ruleMatch = matchRule(activity);
        boolean escalationRequired = requiresEscalation(activity, ruleMatch);

        if (!REVIEW_APPROVED.equals(status) && !REVIEW_REJECTED.equals(status) && !REVIEW_ESCALATED.equals(status)) {
            throw new BusinessException("活动审核状态只能为 approved、rejected 或 escalated");
        }
        if (REVIEW_ESCALATED.equals(status) && !currentUser.hasRole(RoleConstants.ACTIVITY_REVIEWER)) {
            throw new BusinessException("只有活动审核员可以发起升级复审");
        }
        if (REVIEW_APPROVED.equals(status)) {
            if (request.getApprovedRewardPoints() == null) {
                throw new BusinessException("审核通过时必须填写批准积分");
            }
            if (request.getApprovedRewardPoints() > activity.getRequestedRewardPoints()) {
                throw new BusinessException("批准积分不能大于申请积分");
            }
            if (ruleMatch.rule() != null && request.getApprovedRewardPoints() > ruleMatch.rule().getMaxPoints()) {
                throw new BusinessException("批准积分不能超过积分规则允许的最高值");
            }
            if (currentUser.hasRole(RoleConstants.ACTIVITY_REVIEWER)
                && !currentUser.hasRole(RoleConstants.SYSTEM_ADMIN)
                && escalationRequired) {
                throw new BusinessException("该活动命中高风险规则，必须升级到超级管理员复审");
            }
        }

        if (REVIEW_APPROVED.equals(status)) {
            activity.setReviewStatus(REVIEW_APPROVED);
            activity.setApprovedRewardPoints(request.getApprovedRewardPoints());
            activity.setReviewNote(trimToNull(request.getReviewNote()));
            activity.setReviewedBy(currentUser.getUserId());
            activity.setReviewedAt(LocalDateTime.now());
            activity.setPublishDate(LocalDateTime.now());
            activity.setStatus(resolveBusinessStatus(activity.getStartDate(), activity.getEndDate()));
        } else if (REVIEW_REJECTED.equals(status)) {
            activity.setReviewStatus(REVIEW_REJECTED);
            activity.setApprovedRewardPoints(null);
            activity.setReviewNote(trimToNull(request.getReviewNote()));
            activity.setReviewedBy(currentUser.getUserId());
            activity.setReviewedAt(LocalDateTime.now());
            activity.setPublishDate(null);
        } else {
            activity.setReviewStatus(REVIEW_PENDING);
            activity.setReviewNote(trimToNull(request.getReviewNote()));
            activity.setReviewedBy(currentUser.getUserId());
            activity.setReviewedAt(LocalDateTime.now());
        }
        activity.setUpdatedAt(LocalDateTime.now());
        activityMapper.updateById(activity);

        ActivityPublishReview review = new ActivityPublishReview();
        review.setActivityId(activity.getActivityId());
        review.setReviewerId(currentUser.getUserId());
        review.setReviewerRole(resolveActivityReviewerRole(currentUser));
        review.setReviewAction(status);
        review.setRequestedRewardPoints(activity.getRequestedRewardPoints());
        review.setApprovedRewardPoints(REVIEW_APPROVED.equals(status) ? request.getApprovedRewardPoints() : null);
        review.setReviewNote(trimToNull(request.getReviewNote()));
        review.setIsEscalated(REVIEW_ESCALATED.equals(status));
        review.setCreatedAt(LocalDateTime.now());
        activityPublishReviewMapper.insert(review);

        return toActivityVO(activity, findOrganizationName(activity.getOrganizationId()));
    }

    @Override
    public List<AdminVOs.ProductReviewVO> listProductReviews(String status) {
        requireProductReviewer();
        LambdaQueryWrapper<Product> query = new LambdaQueryWrapper<Product>().orderByDesc(Product::getUpdatedAt);
        if (status == null || status.isBlank()) {
            query.eq(Product::getReviewStatus, REVIEW_PENDING);
        } else {
            query.eq(Product::getReviewStatus, status.trim());
        }

        List<Product> products = productMapper.selectList(query);
        Map<Integer, String> organizationNames = organizationMapper.selectBatchIds(
            products.stream().map(Product::getOrganizationId).filter(Objects::nonNull).distinct().toList()
        ).stream().filter(Objects::nonNull)
            .collect(LinkedHashMap::new, (map, organization) -> map.put(organization.getOrganizationId(), organization.getOrganizationName()), Map::putAll);
        Map<Integer, User> userMap = userMapper.selectList(new LambdaQueryWrapper<User>()
                .in(!products.isEmpty(), User::getUserId, products.stream().map(Product::getCreatedBy).filter(Objects::nonNull).distinct().toList()))
            .stream()
            .collect(LinkedHashMap::new, (map, user) -> map.put(user.getUserId(), user), Map::putAll);

        return products.stream()
            .map(product -> toProductReviewVO(product, organizationNames.get(product.getOrganizationId()), userMap.get(product.getCreatedBy())))
            .toList();
    }

    @Override
    @Transactional
    public PlatformVOs.ProductVO reviewProduct(Integer productId, AdminRequests.ProductReviewRequest request) {
        AuthUser currentUser = requireProductReviewer();
        Product product = requireProduct(productId);
        ensureNotSelfReview(currentUser, product.getOrganizationId(), "不能审核自己所属组织提交的商品");

        String status = normalizeStatus(request.getStatus());
        if (!REVIEW_APPROVED.equals(status) && !REVIEW_REJECTED.equals(status) && !PRODUCT_OFF_SHELF.equals(status)) {
            throw new BusinessException("商品审核状态只能为 approved、rejected 或 off_shelf");
        }
        if (!PRODUCT_OFF_SHELF.equals(status) && !REVIEW_PENDING.equals(product.getReviewStatus())) {
            throw new BusinessException("只有待审核商品才能执行通过或驳回");
        }
        if (PRODUCT_OFF_SHELF.equals(status) && REVIEW_DRAFT.equals(product.getReviewStatus())) {
            throw new BusinessException("草稿商品不能直接下架");
        }

        product.setReviewStatus(status);
        product.setReviewNote(trimToNull(request.getReviewNote()));
        product.setReviewedBy(currentUser.getUserId());
        product.setReviewedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);

        ProductReview review = new ProductReview();
        review.setProductId(product.getProductId());
        review.setReviewerId(currentUser.getUserId());
        review.setReviewAction(status);
        review.setReviewNote(trimToNull(request.getReviewNote()));
        review.setCreatedAt(LocalDateTime.now());
        productReviewMapper.insert(review);

        return toProductVO(product, findOrganizationName(product.getOrganizationId()));
    }

    @Override
    public List<AdminVOs.PointsRuleVO> listPointsRules() {
        requireSystemAdmin();
        return pointsRuleConfigMapper.selectList(new LambdaQueryWrapper<PointsRuleConfig>()
                .orderByAsc(PointsRuleConfig::getActivityCategory)
                .orderByAsc(PointsRuleConfig::getMinServiceHours))
            .stream()
            .map(this::toPointsRuleVO)
            .toList();
    }

    @Override
    @Transactional
    public AdminVOs.PointsRuleVO createPointsRule(AdminRequests.PointsRuleSaveRequest request) {
        AuthUser currentUser = requireSystemAdmin();
        validatePointsRuleRequest(request);
        PointsRuleConfig rule = new PointsRuleConfig();
        applyPointsRuleRequest(rule, request, currentUser.getUserId());
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        pointsRuleConfigMapper.insert(rule);
        return toPointsRuleVO(rule);
    }

    @Override
    @Transactional
    public AdminVOs.PointsRuleVO updatePointsRule(Integer id, AdminRequests.PointsRuleSaveRequest request) {
        AuthUser currentUser = requireSystemAdmin();
        validatePointsRuleRequest(request);
        PointsRuleConfig rule = requirePointsRule(id);
        applyPointsRuleRequest(rule, request, currentUser.getUserId());
        rule.setUpdatedAt(LocalDateTime.now());
        pointsRuleConfigMapper.updateById(rule);
        return toPointsRuleVO(rule);
    }

    @Override
    public AdminVOs.PointsMaintenanceResultVO syncPointBalances() {
        requireSystemAdmin();
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().orderByAsc(User::getUserId));
        AdminVOs.PointsMaintenanceResultVO result = new AdminVOs.PointsMaintenanceResultVO();
        result.setScannedUsers(users.size());
        result.setMigratedUsers(0);
        result.setSyncedUsers(0);
        result.setSkippedUsers(0);
        result.setFailedUsers(0);
        result.setMessages(new ArrayList<>());

        for (User user : users) {
            try {
                Integer chainBalance = pointBlockchainService.getBalance(user.getUserId());
                Integer cachedBalance = user.getTotalPoints() == null ? 0 : user.getTotalPoints();
                if (!Objects.equals(chainBalance, cachedBalance)) {
                    userMapper.updateTotalPoints(user.getUserId(), chainBalance);
                    result.setSyncedUsers(result.getSyncedUsers() + 1);
                } else {
                    result.setSkippedUsers(result.getSkippedUsers() + 1);
                }
            } catch (Exception ex) {
                result.setFailedUsers(result.getFailedUsers() + 1);
                result.getMessages().add("userId=" + user.getUserId() + ": " + ex.getMessage());
            }
        }
        return result;
    }

    @Override
    public AdminVOs.PointsMaintenanceResultVO migratePointsToChain() {
        AuthUser currentUser = requireSystemAdmin();
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
            .gt(User::getTotalPoints, 0)
            .orderByAsc(User::getUserId));
        AdminVOs.PointsMaintenanceResultVO result = new AdminVOs.PointsMaintenanceResultVO();
        result.setScannedUsers(users.size());
        result.setMigratedUsers(0);
        result.setSyncedUsers(0);
        result.setSkippedUsers(0);
        result.setFailedUsers(0);
        result.setMessages(new ArrayList<>());

        for (User user : users) {
            String bizKey = "migration:init:" + user.getUserId();
            Long existing = pointsRecordMapper.selectCount(new LambdaQueryWrapper<PointsRecord>()
                .eq(PointsRecord::getBizKey, bizKey));
            if (existing != null && existing > 0) {
                result.setSkippedUsers(result.getSkippedUsers() + 1);
                continue;
            }

            try {
                int points = user.getTotalPoints() == null ? 0 : user.getTotalPoints();
                if (points <= 0) {
                    result.setSkippedUsers(result.getSkippedUsers() + 1);
                    continue;
                }
                PointBlockchainService.PointChainResult pointResult = pointBlockchainService.creditPoints(
                    new PointBlockchainService.PointChainRequest(
                        bizKey,
                        user.getUserId(),
                        user.getOrganizationId(),
                        points,
                        "Legacy points migration",
                        "migration_init",
                        user.getUserId()
                    )
                );
                userMapper.updateTotalPoints(user.getUserId(), pointResult.chainBalanceAfter());

                PointsRecord record = new PointsRecord();
                record.setUserId(user.getUserId());
                record.setOrganizationId(user.getOrganizationId());
                record.setPoints(points);
                record.setTransactionType("earned");
                record.setSource("Legacy points migration");
                record.setReferenceType("migration_init");
                record.setReferenceId(user.getUserId());
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
                result.setMigratedUsers(result.getMigratedUsers() + 1);
            } catch (Exception ex) {
                result.setFailedUsers(result.getFailedUsers() + 1);
                result.getMessages().add("userId=" + user.getUserId() + ": " + ex.getMessage());
            }
        }
        if (result.getMigratedUsers() > 0) {
            result.getMessages().add("operatorUserId=" + currentUser.getUserId());
        }
        return result;
    }

    private void applyPointsRuleRequest(PointsRuleConfig rule, AdminRequests.PointsRuleSaveRequest request, Integer userId) {
        rule.setActivityCategory(request.getActivityCategory().trim());
        rule.setMinServiceHours(request.getMinServiceHours());
        rule.setMaxServiceHours(request.getMaxServiceHours());
        rule.setSuggestedPoints(request.getSuggestedPoints());
        rule.setMaxPoints(request.getMaxPoints());
        rule.setEscalationThreshold(request.getEscalationThreshold());
        rule.setStatus(request.getStatus());
        if (rule.getCreatedBy() == null) {
            rule.setCreatedBy(userId);
        }
        rule.setUpdatedBy(userId);
    }

    private void validatePointsRuleRequest(AdminRequests.PointsRuleSaveRequest request) {
        if (request.getMaxServiceHours().compareTo(request.getMinServiceHours()) <= 0) {
            throw new BusinessException("最大服务时长必须大于最小服务时长");
        }
        if (request.getSuggestedPoints() > request.getMaxPoints()) {
            throw new BusinessException("建议积分不能大于最高积分");
        }
        if (request.getEscalationThreshold() > request.getMaxPoints()) {
            throw new BusinessException("复审阈值不能大于最高积分");
        }
    }

    private RuleMatch matchRule(Activity activity) {
        BigDecimal hours = calculateServiceHours(activity);
        String category = extractPrimaryCategory(activity.getCategoryTags());
        PointsRuleConfig exactRule = pointsRuleConfigMapper.selectOne(new LambdaQueryWrapper<PointsRuleConfig>()
            .eq(PointsRuleConfig::getStatus, 1)
            .eq(PointsRuleConfig::getActivityCategory, category)
            .le(PointsRuleConfig::getMinServiceHours, hours)
            .ge(PointsRuleConfig::getMaxServiceHours, hours)
            .last("LIMIT 1"));
        if (exactRule != null) {
            return new RuleMatch(exactRule, hours);
        }

        PointsRuleConfig fallbackRule = pointsRuleConfigMapper.selectOne(new LambdaQueryWrapper<PointsRuleConfig>()
            .eq(PointsRuleConfig::getStatus, 1)
            .eq(PointsRuleConfig::getActivityCategory, "GENERAL")
            .le(PointsRuleConfig::getMinServiceHours, hours)
            .ge(PointsRuleConfig::getMaxServiceHours, hours)
            .last("LIMIT 1"));
        return new RuleMatch(fallbackRule, hours);
    }

    private boolean requiresEscalation(Activity activity, RuleMatch ruleMatch) {
        if (ruleMatch.rule() == null) {
            return activity.getRequestedRewardPoints() != null && activity.getRequestedRewardPoints() >= 100;
        }
        return activity.getRequestedRewardPoints() != null
            && activity.getRequestedRewardPoints() > ruleMatch.rule().getEscalationThreshold();
    }

    private BigDecimal calculateServiceHours(Activity activity) {
        long minutes = Duration.between(activity.getStartDate(), activity.getEndDate()).toMinutes();
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private String extractPrimaryCategory(String categoryTags) {
        if (categoryTags == null || categoryTags.isBlank()) {
            return "GENERAL";
        }
        String[] parts = categoryTags.split("[,，]");
        String category = parts.length == 0 ? categoryTags : parts[0];
        return category == null || category.isBlank() ? "GENERAL" : category.trim();
    }

    private AdminVOs.AdminUserVO toAdminUserVO(User user, Map<Integer, String> organizationNames) {
        AuthUser authUser = rbacService.buildAuthUser(user);
        AdminVOs.AdminUserVO vo = new AdminVOs.AdminUserVO();
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setOrganizationId(user.getOrganizationId());
        vo.setOrganizationName(user.getOrganizationId() == null ? null : organizationNames.get(user.getOrganizationId()));
        vo.setPrimaryRole(authUser.getPrimaryRole());
        vo.setRoles(new ArrayList<>(authUser.getRoles()));
        vo.setTotalPoints(user.getTotalPoints());
        return vo;
    }

    private AdminVOs.RoleVO toRoleVO(Role role) {
        AdminVOs.RoleVO vo = new AdminVOs.RoleVO();
        vo.setId(role.getId());
        vo.setCode(role.getCode());
        vo.setName(role.getName());
        vo.setStatus(role.getStatus());
        vo.setIsSystem(role.getIsSystem());
        vo.setDataScope(role.getDataScope());
        vo.setSortOrder(role.getSortOrder());
        return vo;
    }

    private AdminVOs.PermissionVO toPermissionVO(Permission permission) {
        AdminVOs.PermissionVO vo = new AdminVOs.PermissionVO();
        vo.setId(permission.getId());
        vo.setCode(permission.getCode());
        vo.setName(permission.getName());
        vo.setModule(permission.getModule());
        vo.setHttpMethod(permission.getHttpMethod());
        vo.setApiPattern(permission.getApiPattern());
        vo.setStatus(permission.getStatus());
        return vo;
    }

    private AdminVOs.ActivityReviewVO toActivityReviewVO(Activity activity, String organizationName, User submitter, RuleMatch ruleMatch) {
        AdminVOs.ActivityReviewVO vo = new AdminVOs.ActivityReviewVO();
        vo.setActivityId(activity.getActivityId());
        vo.setOrganizationId(activity.getOrganizationId());
        vo.setOrganizationName(organizationName);
        vo.setActivityName(activity.getActivityName());
        vo.setStartDate(format(activity.getStartDate()));
        vo.setEndDate(format(activity.getEndDate()));
        vo.setLocation(activity.getLocation());
        vo.setCategoryTags(activity.getCategoryTags());
        vo.setRequestedRewardPoints(activity.getRequestedRewardPoints());
        vo.setApprovedRewardPoints(activity.getApprovedRewardPoints());
        vo.setReviewStatus(activity.getReviewStatus());
        vo.setReviewNote(activity.getReviewNote());
        vo.setRecommendedPoints(ruleMatch.rule() == null ? null : ruleMatch.rule().getSuggestedPoints());
        vo.setMaxPoints(ruleMatch.rule() == null ? null : ruleMatch.rule().getMaxPoints());
        vo.setEscalationThreshold(ruleMatch.rule() == null ? null : ruleMatch.rule().getEscalationThreshold());
        vo.setEscalationRequired(requiresEscalation(activity, ruleMatch));
        vo.setSubmittedBy(submitter == null ? "未知用户" : submitter.getUsername());
        vo.setSubmittedAt(format(activity.getUpdatedAt()));
        return vo;
    }

    private AdminVOs.ProductReviewVO toProductReviewVO(Product product, String organizationName, User submitter) {
        AdminVOs.ProductReviewVO vo = new AdminVOs.ProductReviewVO();
        vo.setProductId(product.getProductId());
        vo.setOrganizationId(product.getOrganizationId());
        vo.setOrganizationName(organizationName);
        vo.setProductName(product.getProductName());
        vo.setProductDescription(product.getProductDescription());
        vo.setPrice(product.getPrice());
        vo.setStock(product.getStock());
        vo.setImagePath(product.getImagePath());
        vo.setReviewStatus(product.getReviewStatus());
        vo.setReviewNote(product.getReviewNote());
        vo.setSubmittedBy(submitter == null ? "未知用户" : submitter.getUsername());
        vo.setSubmittedAt(format(product.getCreatedAt()));
        return vo;
    }

    private AdminVOs.PointsRuleVO toPointsRuleVO(PointsRuleConfig rule) {
        AdminVOs.PointsRuleVO vo = new AdminVOs.PointsRuleVO();
        vo.setId(rule.getId());
        vo.setActivityCategory(rule.getActivityCategory());
        vo.setMinServiceHours(rule.getMinServiceHours());
        vo.setMaxServiceHours(rule.getMaxServiceHours());
        vo.setSuggestedPoints(rule.getSuggestedPoints());
        vo.setMaxPoints(rule.getMaxPoints());
        vo.setEscalationThreshold(rule.getEscalationThreshold());
        vo.setStatus(rule.getStatus());
        vo.setUpdatedAt(format(rule.getUpdatedAt()));
        return vo;
    }

    private PlatformVOs.ActivityVO toActivityVO(Activity activity, String organizationName) {
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
        return vo;
    }

    private PlatformVOs.ProductVO toProductVO(Product product, String organizationName) {
        PlatformVOs.ProductVO vo = new PlatformVOs.ProductVO();
        vo.setProductId(product.getProductId());
        vo.setOrganizationId(product.getOrganizationId());
        vo.setOrganizationName(organizationName);
        vo.setProductName(product.getProductName());
        vo.setProductDescription(product.getProductDescription());
        vo.setPrice(product.getPrice());
        vo.setStock(product.getStock());
        vo.setImagePath(product.getImagePath());
        vo.setReviewStatus(product.getReviewStatus());
        vo.setReviewNote(product.getReviewNote());
        vo.setReviewedAt(format(product.getReviewedAt()));
        return vo;
    }

    private User requireUser(Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private Activity requireActivity(Integer activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        return activity;
    }

    private Product requireProduct(Integer productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        return product;
    }

    private PointsRuleConfig requirePointsRule(Integer id) {
        PointsRuleConfig rule = pointsRuleConfigMapper.selectById(id);
        if (rule == null) {
            throw new BusinessException("积分规则不存在");
        }
        return rule;
    }

    private AuthUser requireActivityReviewer() {
        AuthUser currentUser = UserContext.getRequiredUser();
        if (!currentUser.hasRole(RoleConstants.ACTIVITY_REVIEWER) && !currentUser.hasRole(RoleConstants.SYSTEM_ADMIN)) {
            throw new BusinessException("当前账号没有活动审核权限");
        }
        return currentUser;
    }

    private AuthUser requireProductReviewer() {
        AuthUser currentUser = UserContext.getRequiredUser();
        if (!currentUser.hasRole(RoleConstants.PRODUCT_REVIEWER) && !currentUser.hasRole(RoleConstants.SYSTEM_ADMIN)) {
            throw new BusinessException("当前账号没有商品审核权限");
        }
        return currentUser;
    }

    private AuthUser requireSystemAdmin() {
        AuthUser currentUser = UserContext.getRequiredUser();
        if (!currentUser.hasRole(RoleConstants.SYSTEM_ADMIN)) {
            throw new BusinessException("当前账号没有系统管理员权限");
        }
        return currentUser;
    }

    private void ensureNotSelfReview(AuthUser currentUser, Integer targetOrganizationId, String message) {
        if (currentUser.getOrganizationId() != null && Objects.equals(currentUser.getOrganizationId(), targetOrganizationId)) {
            throw new BusinessException(message);
        }
    }

    private Map<Integer, String> mapOrganizationNames(List<User> users) {
        List<Integer> organizationIds = users.stream()
            .map(User::getOrganizationId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (organizationIds.isEmpty()) {
            return Map.of();
        }
        return organizationMapper.selectBatchIds(organizationIds).stream()
            .filter(Objects::nonNull)
            .collect(LinkedHashMap::new, (map, organization) -> map.put(organization.getOrganizationId(), organization.getOrganizationName()), Map::putAll);
    }

    private String findOrganizationName(Integer organizationId) {
        if (organizationId == null) {
            return null;
        }
        Organization organization = organizationMapper.selectById(organizationId);
        return organization == null ? null : organization.getOrganizationName();
    }

    private String resolveActivityReviewerRole(AuthUser currentUser) {
        return currentUser.hasRole(RoleConstants.SYSTEM_ADMIN) ? RoleConstants.SYSTEM_ADMIN : RoleConstants.ACTIVITY_REVIEWER;
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

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toLowerCase();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String format(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_FORMATTER);
    }

    private record RuleMatch(PointsRuleConfig rule, BigDecimal hours) {
    }
}
