package com.gzu.volunteerblockchain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gzu.volunteerblockchain.common.AuthUser;
import com.gzu.volunteerblockchain.common.RoleConstants;
import com.gzu.volunteerblockchain.common.UserContext;
import com.gzu.volunteerblockchain.dto.PlatformRequests;
import com.gzu.volunteerblockchain.entity.Organization;
import com.gzu.volunteerblockchain.entity.PointsRecord;
import com.gzu.volunteerblockchain.entity.Product;
import com.gzu.volunteerblockchain.entity.ProductRedemption;
import com.gzu.volunteerblockchain.entity.ProductReview;
import com.gzu.volunteerblockchain.entity.User;
import com.gzu.volunteerblockchain.exception.BusinessException;
import com.gzu.volunteerblockchain.mapper.OrganizationMapper;
import com.gzu.volunteerblockchain.mapper.PointsRecordMapper;
import com.gzu.volunteerblockchain.mapper.ProductMapper;
import com.gzu.volunteerblockchain.mapper.ProductRedemptionMapper;
import com.gzu.volunteerblockchain.mapper.ProductReviewMapper;
import com.gzu.volunteerblockchain.mapper.UserMapper;
import com.gzu.volunteerblockchain.service.PointBlockchainService;
import com.gzu.volunteerblockchain.service.ProductService;
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

@Service
public class ProductServiceImpl implements ProductService {

    private static final String REVIEW_DRAFT = "draft";
    private static final String REVIEW_PENDING = "pending_review";
    private static final String REVIEW_APPROVED = "approved";
    private static final String REDEMPTION_PENDING = "pending";
    private static final String REDEMPTION_FULFILLED = "fulfilled";
    private static final String REDEMPTION_CANCELLED = "cancelled";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ProductMapper productMapper;
    private final ProductReviewMapper productReviewMapper;
    private final ProductRedemptionMapper redemptionMapper;
    private final PointsRecordMapper pointsRecordMapper;
    private final OrganizationMapper organizationMapper;
    private final UserMapper userMapper;
    private final PointBlockchainService pointBlockchainService;

    public ProductServiceImpl(
        ProductMapper productMapper,
        ProductReviewMapper productReviewMapper,
        ProductRedemptionMapper redemptionMapper,
        PointsRecordMapper pointsRecordMapper,
        OrganizationMapper organizationMapper,
        UserMapper userMapper,
        PointBlockchainService pointBlockchainService
    ) {
        this.productMapper = productMapper;
        this.productReviewMapper = productReviewMapper;
        this.redemptionMapper = redemptionMapper;
        this.pointsRecordMapper = pointsRecordMapper;
        this.organizationMapper = organizationMapper;
        this.userMapper = userMapper;
        this.pointBlockchainService = pointBlockchainService;
    }

    @Override
    @Transactional
    public PlatformVOs.ProductVO createProduct(PlatformRequests.ProductSaveRequest request) {
        AuthUser currentUser = requireOrganizationAdmin();
        Product product = new Product();
        applyProductRequest(product, request);
        product.setOrganizationId(currentUser.getOrganizationId());
        product.setCreatedBy(currentUser.getUserId());
        product.setReviewStatus(REVIEW_DRAFT);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.insert(product);
        return toProductVO(product, findOrganizationName(product.getOrganizationId()));
    }

    @Override
    @Transactional
    public PlatformVOs.ProductVO updateProduct(Integer id, PlatformRequests.ProductSaveRequest request) {
        AuthUser currentUser = requireOrganizationAdmin();
        Product product = requireProduct(id);
        ensureSameOrganization(currentUser.getOrganizationId(), product.getOrganizationId(), "不能修改其他组织的商品");

        boolean sensitiveChanged = hasSensitiveChanges(product, request);
        applyProductRequest(product, request);
        if (REVIEW_APPROVED.equals(product.getReviewStatus()) && sensitiveChanged) {
            product.setReviewStatus(REVIEW_PENDING);
            product.setReviewNote(null);
            product.setReviewedBy(null);
            product.setReviewedAt(null);
        }
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
        return toProductVO(product, findOrganizationName(product.getOrganizationId()));
    }

    @Override
    @Transactional
    public void deleteProduct(Integer id) {
        AuthUser currentUser = requireOrganizationAdmin();
        Product product = requireProduct(id);
        ensureSameOrganization(currentUser.getOrganizationId(), product.getOrganizationId(), "不能删除其他组织的商品");
        Long redemptionCount = redemptionMapper.selectCount(new LambdaQueryWrapper<ProductRedemption>()
            .eq(ProductRedemption::getProductId, id));
        if (redemptionCount != null && redemptionCount > 0) {
            throw new BusinessException("已有兑换记录的商品不能删除");
        }
        productMapper.deleteById(id);
    }

    @Override
    @Transactional
    public PlatformVOs.ProductVO submitProduct(Integer id) {
        AuthUser currentUser = requireOrganizationAdmin();
        Product product = requireProduct(id);
        ensureSameOrganization(currentUser.getOrganizationId(), product.getOrganizationId(), "不能提交其他组织的商品");
        if (REVIEW_PENDING.equals(product.getReviewStatus())) {
            throw new BusinessException("商品已在审核中");
        }

        product.setReviewStatus(REVIEW_PENDING);
        product.setReviewNote(null);
        product.setReviewedBy(null);
        product.setReviewedAt(null);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);

        ProductReview review = new ProductReview();
        review.setProductId(product.getProductId());
        review.setReviewerId(currentUser.getUserId());
        review.setReviewAction("submitted");
        review.setReviewNote("组织提交商品审核");
        review.setCreatedAt(LocalDateTime.now());
        productReviewMapper.insert(review);
        return toProductVO(product, findOrganizationName(product.getOrganizationId()));
    }

    @Override
    public List<PlatformVOs.ProductVO> listVolunteerProducts() {
        requireVolunteer();
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
            .eq(Product::getReviewStatus, REVIEW_APPROVED)
            .orderByDesc(Product::getUpdatedAt));
        Map<Integer, String> organizationNames = mapOrganizationNames(products.stream().map(Product::getOrganizationId).toList());
        return products.stream().map(product -> toProductVO(product, organizationNames.get(product.getOrganizationId()))).toList();
    }

    @Override
    public List<PlatformVOs.ProductVO> listOrganizationProducts() {
        AuthUser currentUser = requireOrganizationAdmin();
        String organizationName = findOrganizationName(currentUser.getOrganizationId());
        return productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getOrganizationId, currentUser.getOrganizationId())
                .orderByDesc(Product::getCreatedAt))
            .stream()
            .map(product -> toProductVO(product, organizationName))
            .toList();
    }

    @Override
    @Transactional
    public PlatformVOs.RedemptionVO redeemProduct(PlatformRequests.RedemptionRequest request) {
        AuthUser currentUser = requireVolunteer();
        Product product = requireProduct(request.getProductId());
        if (!REVIEW_APPROVED.equals(product.getReviewStatus())) {
            throw new BusinessException("该商品尚未通过审核，不能兑换");
        }

        if (product.getPrice() == null || product.getPrice() <= 0) {
            throw new BusinessException("商品积分价格配置异常，无法兑换");
        }
        if (productMapper.decrementStock(product.getProductId()) == 0) {
            throw new BusinessException("商品库存不足");
        }

        ProductRedemption redemption = new ProductRedemption();
        redemption.setUserId(currentUser.getUserId());
        redemption.setProductId(product.getProductId());
        redemption.setOrganizationId(product.getOrganizationId());
        redemption.setPointsCost(product.getPrice());
        redemption.setStatus(REDEMPTION_PENDING);
        redemption.setCreatedAt(LocalDateTime.now());
        redemptionMapper.insert(redemption);

        PointBlockchainService.PointChainResult pointResult = pointBlockchainService.debitPoints(
            new PointBlockchainService.PointChainRequest(
                "redemption:" + redemption.getId() + ":spend:" + currentUser.getUserId(),
                currentUser.getUserId(),
                product.getOrganizationId(),
                product.getPrice(),
                "Unified points mall redemption",
                "redemption",
                redemption.getId()
            )
        );
        userMapper.updateTotalPoints(currentUser.getUserId(), pointResult.chainBalanceAfter());

        PointsRecord record = new PointsRecord();
        record.setUserId(currentUser.getUserId());
        record.setOrganizationId(product.getOrganizationId());
        record.setPoints(product.getPrice());
        record.setTransactionType("spent");
        record.setSource("统一积分商城兑换");
        record.setReferenceType("redemption");
        record.setReferenceId(redemption.getId());
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

        return toRedemptionVO(redemption, product, findOrganizationName(product.getOrganizationId()), requireUser(currentUser.getUserId()));
    }

    @Override
    @Transactional
    public PlatformVOs.RedemptionVO updateRedemptionStatus(Integer id, PlatformRequests.RedemptionStatusRequest request) {
        AuthUser currentUser = requireOrganizationAdmin();
        ProductRedemption redemption = requireRedemption(id);
        ensureSameOrganization(currentUser.getOrganizationId(), redemption.getOrganizationId(), "不能处理其他组织的兑换记录");
        if (!REDEMPTION_PENDING.equals(redemption.getStatus())) {
            throw new BusinessException("只有待处理兑换记录才能更新状态");
        }

        String status = normalizeStatus(request.getStatus());
        if (!REDEMPTION_FULFILLED.equals(status) && !REDEMPTION_CANCELLED.equals(status)) {
            throw new BusinessException("兑换状态只能更新为 fulfilled 或 cancelled");
        }

        redemption.setStatus(status);
        redemption.setHandledAt(LocalDateTime.now());
        redemptionMapper.updateById(redemption);

        if (REDEMPTION_CANCELLED.equals(status)) {
            PointBlockchainService.PointChainResult pointResult = pointBlockchainService.refundPoints(
                new PointBlockchainService.PointChainRequest(
                    "redemption:" + redemption.getId() + ":refund:" + redemption.getUserId(),
                    redemption.getUserId(),
                    redemption.getOrganizationId(),
                    redemption.getPointsCost(),
                    "Redemption cancelled refund",
                    "redemption",
                    redemption.getId()
                )
            );
            userMapper.updateTotalPoints(redemption.getUserId(), pointResult.chainBalanceAfter());
            Product product = requireProduct(redemption.getProductId());
            productMapper.incrementStock(product.getProductId());

            PointsRecord refund = new PointsRecord();
            refund.setUserId(redemption.getUserId());
            refund.setOrganizationId(redemption.getOrganizationId());
            refund.setPoints(redemption.getPointsCost());
            refund.setTransactionType(pointResult.transactionType());
            refund.setSource("兑换取消返还");
            refund.setReferenceType("redemption");
            refund.setReferenceId(redemption.getId());
            refund.setBizKey(pointResult.bizKey());
            refund.setDigest(pointResult.digest());
            refund.setTxHash(pointResult.txHash());
            refund.setBlockNumber(pointResult.blockNumber());
            refund.setContractName(pointResult.contractName());
            refund.setOnchainStatus("success");
            refund.setOnchainAt(pointResult.onchainAt());
            refund.setChainBalanceAfter(pointResult.chainBalanceAfter());
            refund.setCreatedAt(LocalDateTime.now());
            pointsRecordMapper.insert(refund);
        }

        Product product = requireProduct(redemption.getProductId());
        return toRedemptionVO(redemption, product, findOrganizationName(product.getOrganizationId()), requireUser(redemption.getUserId()));
    }

    @Override
    public List<PlatformVOs.RedemptionVO> listMyRedemptions() {
        AuthUser currentUser = requireVolunteer();
        List<ProductRedemption> redemptions = redemptionMapper.selectList(new LambdaQueryWrapper<ProductRedemption>()
            .eq(ProductRedemption::getUserId, currentUser.getUserId())
            .orderByDesc(ProductRedemption::getCreatedAt));
        Map<Integer, Product> productMap = mapProducts(redemptions.stream().map(ProductRedemption::getProductId).toList());
        Map<Integer, String> organizationNames = mapOrganizationNames(productMap.values().stream().map(Product::getOrganizationId).toList());
        User user = requireUser(currentUser.getUserId());
        return redemptions.stream()
            .map(item -> {
                Product product = productMap.get(item.getProductId());
                String organizationName = product == null ? null : organizationNames.get(product.getOrganizationId());
                return toRedemptionVO(item, product, organizationName, user);
            })
            .toList();
    }

    @Override
    public List<PlatformVOs.RedemptionVO> listOrganizationRedemptions() {
        AuthUser currentUser = requireOrganizationAdmin();
        List<ProductRedemption> redemptions = redemptionMapper.selectList(new LambdaQueryWrapper<ProductRedemption>()
            .eq(ProductRedemption::getOrganizationId, currentUser.getOrganizationId())
            .orderByDesc(ProductRedemption::getCreatedAt));
        Map<Integer, Product> productMap = mapProducts(redemptions.stream().map(ProductRedemption::getProductId).toList());
        Map<Integer, User> userMap = mapUsers(redemptions.stream().map(ProductRedemption::getUserId).toList());
        String organizationName = findOrganizationName(currentUser.getOrganizationId());
        return redemptions.stream()
            .map(item -> toRedemptionVO(item, productMap.get(item.getProductId()), organizationName, userMap.get(item.getUserId())))
            .toList();
    }

    private void applyProductRequest(Product product, PlatformRequests.ProductSaveRequest request) {
        product.setProductName(trimRequired(request.getProductName(), "商品名称不能为空"));
        product.setProductDescription(trimToNull(request.getProductDescription()));
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setImagePath(trimToNull(request.getImagePath()));
    }

    private boolean hasSensitiveChanges(Product product, PlatformRequests.ProductSaveRequest request) {
        return !Objects.equals(trimToNull(product.getProductName()), trimToNull(request.getProductName()))
            || !Objects.equals(trimToNull(product.getProductDescription()), trimToNull(request.getProductDescription()))
            || !Objects.equals(product.getPrice(), request.getPrice())
            || !Objects.equals(trimToNull(product.getImagePath()), trimToNull(request.getImagePath()));
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

    private PlatformVOs.RedemptionVO toRedemptionVO(ProductRedemption redemption, Product product, String organizationName, User user) {
        PlatformVOs.RedemptionVO vo = new PlatformVOs.RedemptionVO();
        vo.setId(redemption.getId());
        vo.setProductId(redemption.getProductId());
        vo.setProductName(product == null ? "未知商品" : product.getProductName());
        vo.setOrganizationId(redemption.getOrganizationId());
        vo.setOrganizationName(organizationName);
        vo.setUserId(redemption.getUserId());
        vo.setUsername(user == null ? "未知用户" : user.getUsername());
        vo.setPointsCost(redemption.getPointsCost());
        vo.setStatus(redemption.getStatus());
        vo.setCreatedAt(format(redemption.getCreatedAt()));
        vo.setHandledAt(format(redemption.getHandledAt()));
        return vo;
    }

    private Map<Integer, Product> mapProducts(List<Integer> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        return productMapper.selectList(new LambdaQueryWrapper<Product>().in(Product::getProductId, productIds)).stream()
            .collect(Collectors.toMap(Product::getProductId, Function.identity(), (left, right) -> left));
    }

    private Map<Integer, User> mapUsers(List<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getUserId, userIds)).stream()
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

    private Product requireProduct(Integer id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        return product;
    }

    private ProductRedemption requireRedemption(Integer id) {
        ProductRedemption redemption = redemptionMapper.selectById(id);
        if (redemption == null) {
            throw new BusinessException("兑换记录不存在");
        }
        return redemption;
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
}
