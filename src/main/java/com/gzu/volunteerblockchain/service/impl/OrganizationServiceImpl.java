package com.gzu.volunteerblockchain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gzu.volunteerblockchain.common.AuthUser;
import com.gzu.volunteerblockchain.common.RoleConstants;
import com.gzu.volunteerblockchain.common.UserContext;
import com.gzu.volunteerblockchain.dto.PlatformRequests;
import com.gzu.volunteerblockchain.entity.Activity;
import com.gzu.volunteerblockchain.entity.Organization;
import com.gzu.volunteerblockchain.entity.OrganizationJoinRequest;
import com.gzu.volunteerblockchain.entity.User;
import com.gzu.volunteerblockchain.exception.BusinessException;
import com.gzu.volunteerblockchain.mapper.ActivityMapper;
import com.gzu.volunteerblockchain.mapper.OrganizationJoinRequestMapper;
import com.gzu.volunteerblockchain.mapper.OrganizationMapper;
import com.gzu.volunteerblockchain.mapper.UserMapper;
import com.gzu.volunteerblockchain.service.OrganizationService;
import com.gzu.volunteerblockchain.vo.PlatformVOs;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OrganizationServiceImpl implements OrganizationService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OrganizationMapper organizationMapper;
    private final OrganizationJoinRequestMapper joinRequestMapper;
    private final UserMapper userMapper;
    private final ActivityMapper activityMapper;

    @Value("${storage.upload-dir:src/main/resources/static/uploads}")
    private String uploadDir;

    @Value("${storage.avatar-dir:src/main/resources/static/uploads/avatars}")
    private String avatarDir;

    public OrganizationServiceImpl(
        OrganizationMapper organizationMapper,
        OrganizationJoinRequestMapper joinRequestMapper,
        UserMapper userMapper,
        ActivityMapper activityMapper
    ) {
        this.organizationMapper = organizationMapper;
        this.joinRequestMapper = joinRequestMapper;
        this.userMapper = userMapper;
        this.activityMapper = activityMapper;
    }

    @Override
    public List<PlatformVOs.OrganizationVO> listOrganizations() {
        return organizationMapper.selectList(new LambdaQueryWrapper<Organization>()
                .orderByAsc(Organization::getOrganizationName))
            .stream()
            .map(this::toOrganizationVO)
            .toList();
    }

    @Override
    @Transactional
    public PlatformVOs.JoinRequestVO createJoinRequest(PlatformRequests.JoinOrganizationRequest request) {
        AuthUser currentUser = requireVolunteer();
        User user = requireUser(currentUser.getUserId());
        if (user.getOrganizationId() != null) {
            throw new BusinessException("你已绑定默认归属组织，不能重复申请");
        }

        Organization organization = requireOrganization(request.getOrganizationId());
        Long sameOrganizationPendingCount = joinRequestMapper.selectCount(new LambdaQueryWrapper<OrganizationJoinRequest>()
            .eq(OrganizationJoinRequest::getUserId, currentUser.getUserId())
            .eq(OrganizationJoinRequest::getOrganizationId, organization.getOrganizationId())
            .eq(OrganizationJoinRequest::getStatus, "pending"));
        if (sameOrganizationPendingCount != null && sameOrganizationPendingCount > 0) {
            throw new BusinessException("你已向该组织提交待审核申请，请勿重复提交");
        }

        OrganizationJoinRequest joinRequest = new OrganizationJoinRequest();
        joinRequest.setUserId(currentUser.getUserId());
        joinRequest.setOrganizationId(organization.getOrganizationId());
        joinRequest.setApplyReason(trimToNull(request.getApplyReason()));
        joinRequest.setStatus("pending");
        joinRequest.setCreatedAt(LocalDateTime.now());
        joinRequestMapper.insert(joinRequest);
        return toJoinRequestVO(joinRequest, user.getUsername(), organization.getOrganizationName());
    }

    @Override
    public List<PlatformVOs.JoinRequestVO> listMyJoinRequests() {
        AuthUser currentUser = requireVolunteer();
        User user = requireUser(currentUser.getUserId());
        Map<Integer, String> organizationNames = organizationMapper.selectList(null).stream()
            .collect(Collectors.toMap(Organization::getOrganizationId, Organization::getOrganizationName, (left, right) -> left));
        return joinRequestMapper.selectList(new LambdaQueryWrapper<OrganizationJoinRequest>()
                .eq(OrganizationJoinRequest::getUserId, currentUser.getUserId())
                .orderByDesc(OrganizationJoinRequest::getCreatedAt))
            .stream()
            .map(item -> toJoinRequestVO(item, user.getUsername(), organizationNames.get(item.getOrganizationId())))
            .toList();
    }

    @Override
    public PlatformVOs.OrganizationVO getMyOrganization() {
        AuthUser currentUser = requireVolunteer();
        User user = requireUser(currentUser.getUserId());
        if (user.getOrganizationId() == null) {
            return null;
        }
        return toOrganizationVO(requireOrganization(user.getOrganizationId()));
    }

    @Override
    @Transactional
    public void leaveMyOrganization() {
        AuthUser currentUser = requireVolunteer();
        User user = requireUser(currentUser.getUserId());
        if (user.getOrganizationId() == null) {
            throw new BusinessException("你暂未加入组织");
        }
        user.setOrganizationId(null);
        userMapper.updateById(user);
    }

    @Override
    public PlatformVOs.OrganizationVO getWorkbenchOrganization() {
        AuthUser currentUser = requireOrganizationAdmin();
        return toOrganizationVO(requireOrganization(currentUser.getOrganizationId()));
    }

    @Override
    @Transactional
    public PlatformVOs.OrganizationVO updateWorkbenchOrganization(PlatformRequests.OrganizationProfileUpdateRequest request) {
        AuthUser currentUser = requireOrganizationAdmin();
        Organization organization = requireOrganization(currentUser.getOrganizationId());
        organization.setOrganizationName(trimRequired(request.getOrganizationName(), "组织名称不能为空"));
        organization.setOrganizationDescription(trimToNull(request.getOrganizationDescription()));
        organizationMapper.updateById(organization);
        return toOrganizationVO(organization);
    }

    @Override
    @Transactional
    public PlatformVOs.OrganizationVO updateWorkbenchOrganizationAvatar(MultipartFile file) {
        String contentType = file == null ? null : file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessException("组织头像文件必须是图片");
        }

        AuthUser currentUser = requireOrganizationAdmin();
        Organization organization = requireOrganization(currentUser.getOrganizationId());
        String oldAvatarPath = organization.getAvatarPath();
        String newAvatarPath = storeAvatar(file);
        try {
            organization.setAvatarPath(newAvatarPath);
            organizationMapper.updateById(organization);
        } catch (RuntimeException ex) {
            deleteUploadedFile(newAvatarPath);
            throw ex;
        }
        deleteUploadedFile(oldAvatarPath);
        return toOrganizationVO(organization);
    }

    @Override
    public List<PlatformVOs.JoinRequestVO> listOrganizationJoinRequests(String status) {
        AuthUser currentUser = requireOrganizationAdmin();
        LambdaQueryWrapper<OrganizationJoinRequest> query = new LambdaQueryWrapper<OrganizationJoinRequest>()
            .eq(OrganizationJoinRequest::getOrganizationId, currentUser.getOrganizationId())
            .orderByDesc(OrganizationJoinRequest::getCreatedAt);
        if (status != null && !status.isBlank()) {
            query.eq(OrganizationJoinRequest::getStatus, status.trim());
        }

        Map<Integer, User> userMap = userMapper.selectList(null).stream()
            .collect(Collectors.toMap(User::getUserId, Function.identity(), (left, right) -> left));
        String organizationName = requireOrganization(currentUser.getOrganizationId()).getOrganizationName();

        return joinRequestMapper.selectList(query).stream()
            .map(item -> toJoinRequestVO(item, safeUsername(userMap.get(item.getUserId())), organizationName))
            .toList();
    }

    @Override
    @Transactional
    public PlatformVOs.JoinRequestVO reviewJoinRequest(Integer id, PlatformRequests.ReviewJoinRequest request) {
        AuthUser currentUser = requireOrganizationAdmin();
        OrganizationJoinRequest joinRequest = requireJoinRequest(id);
        ensureSameOrganization(currentUser.getOrganizationId(), joinRequest.getOrganizationId(), "不能审核其他组织的加入申请");
        if (!"pending".equals(joinRequest.getStatus())) {
            throw new BusinessException("该申请已处理，不能重复审核");
        }

        String status = normalizeStatus(request.getStatus());
        if (!"approved".equals(status) && !"rejected".equals(status)) {
            throw new BusinessException("申请只能审核为 approved 或 rejected");
        }

        joinRequest.setStatus(status);
        joinRequest.setReviewNote(trimToNull(request.getReviewNote()));
        joinRequest.setReviewedBy(currentUser.getUserId());
        joinRequest.setReviewedAt(LocalDateTime.now());
        joinRequestMapper.updateById(joinRequest);

        User user = requireUser(joinRequest.getUserId());
        if ("approved".equals(status)) {
            if (user.getOrganizationId() != null) {
                deleteOtherJoinRequests(joinRequest);
                throw new BusinessException("该用户已绑定默认归属组织");
            }
            user.setOrganizationId(joinRequest.getOrganizationId());
            userMapper.updateById(user);
            deleteOtherJoinRequests(joinRequest);
        }

        return toJoinRequestVO(joinRequest, user.getUsername(), requireOrganization(joinRequest.getOrganizationId()).getOrganizationName());
    }

    private PlatformVOs.OrganizationVO toOrganizationVO(Organization organization) {
        PlatformVOs.OrganizationVO vo = new PlatformVOs.OrganizationVO();
        vo.setOrganizationId(organization.getOrganizationId());
        vo.setOrganizationName(organization.getOrganizationName());
        vo.setOrganizationDescription(organization.getOrganizationDescription());
        vo.setAvatarPath(organization.getAvatarPath());
        vo.setPublicActivityCount(countPublicActivities(organization.getOrganizationId()));
        vo.setVolunteerCount(countVolunteers(organization.getOrganizationId()));
        return vo;
    }

    private void deleteOtherJoinRequests(OrganizationJoinRequest approvedRequest) {
        joinRequestMapper.delete(new LambdaQueryWrapper<OrganizationJoinRequest>()
            .eq(OrganizationJoinRequest::getUserId, approvedRequest.getUserId())
            .ne(OrganizationJoinRequest::getId, approvedRequest.getId()));
    }

    private Integer countVolunteers(Integer organizationId) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
            .eq(User::getOrganizationId, organizationId));
        return count == null ? 0 : count.intValue();
    }

    private Integer countPublicActivities(Integer organizationId) {
        Long count = activityMapper.selectCount(new LambdaQueryWrapper<Activity>()
            .eq(Activity::getOrganizationId, organizationId)
            .eq(Activity::getReviewStatus, "approved"));
        return count == null ? 0 : count.intValue();
    }

    private PlatformVOs.JoinRequestVO toJoinRequestVO(OrganizationJoinRequest item, String username, String organizationName) {
        PlatformVOs.JoinRequestVO vo = new PlatformVOs.JoinRequestVO();
        vo.setId(item.getId());
        vo.setUserId(item.getUserId());
        vo.setUsername(username);
        vo.setOrganizationId(item.getOrganizationId());
        vo.setOrganizationName(organizationName);
        vo.setApplyReason(item.getApplyReason());
        vo.setStatus(item.getStatus());
        vo.setReviewNote(item.getReviewNote());
        vo.setCreatedAt(format(item.getCreatedAt()));
        vo.setReviewedAt(format(item.getReviewedAt()));
        return vo;
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

    private User requireUser(Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private Organization requireOrganization(Integer organizationId) {
        Organization organization = organizationMapper.selectById(organizationId);
        if (organization == null) {
            throw new BusinessException("组织不存在");
        }
        return organization;
    }

    private OrganizationJoinRequest requireJoinRequest(Integer id) {
        OrganizationJoinRequest joinRequest = joinRequestMapper.selectById(id);
        if (joinRequest == null) {
            throw new BusinessException("组织申请不存在");
        }
        return joinRequest;
    }

    private void ensureSameOrganization(Integer expectedOrgId, Integer actualOrgId, String message) {
        if (expectedOrgId == null || actualOrgId == null || !expectedOrgId.equals(actualOrgId)) {
            throw new BusinessException(message);
        }
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toLowerCase();
    }

    private String safeUsername(User user) {
        return user == null ? "未知用户" : user.getUsername();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private String format(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_FORMATTER);
    }

    private String storeAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传组织头像不能为空");
        }

        String extension = resolveImageExtension(file);
        String filename = UUID.randomUUID().toString().replace("-", "") + extension;
        Path basePath = Paths.get(avatarDir).toAbsolutePath().normalize();
        Path targetFile = basePath.resolve(filename).normalize();

        if (!targetFile.startsWith(basePath)) {
            throw new BusinessException("非法组织头像文件路径");
        }

        try {
            Files.createDirectories(basePath);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BusinessException("组织头像保存失败: " + ex.getMessage());
        }

        return "/uploads/avatars/" + filename;
    }

    private String resolveImageExtension(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null) {
            int lastDot = originalName.lastIndexOf('.');
            if (lastDot >= 0 && lastDot < originalName.length() - 1) {
                extension = originalName.substring(lastDot).toLowerCase(Locale.ROOT);
            }
        }

        Set<String> allowedExtensions = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp");
        if (allowedExtensions.contains(extension)) {
            return extension;
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "image/bmp" -> ".bmp";
            default -> ".jpg";
        };
    }

    private void deleteUploadedFile(String publicPath) {
        if (publicPath == null || publicPath.isBlank() || !publicPath.startsWith("/uploads/")) {
            return;
        }

        Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        String relativePath = publicPath.substring("/uploads/".length()).replace('\\', '/');
        Path targetFile = uploadRoot.resolve(relativePath).normalize();
        if (!targetFile.startsWith(uploadRoot)) {
            return;
        }

        try {
            Files.deleteIfExists(targetFile);
        } catch (IOException ex) {
            // 删除旧组织头像失败不影响新头像生效。
        }
    }
}
