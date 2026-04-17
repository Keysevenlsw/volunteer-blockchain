package com.gzu.volunteerblockchain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gzu.volunteerblockchain.common.AuthUser;
import com.gzu.volunteerblockchain.common.RoleConstants;
import com.gzu.volunteerblockchain.common.UserContext;
import com.gzu.volunteerblockchain.dto.PlatformRequests;
import com.gzu.volunteerblockchain.entity.Organization;
import com.gzu.volunteerblockchain.entity.OrganizationJoinRequest;
import com.gzu.volunteerblockchain.entity.User;
import com.gzu.volunteerblockchain.exception.BusinessException;
import com.gzu.volunteerblockchain.mapper.OrganizationJoinRequestMapper;
import com.gzu.volunteerblockchain.mapper.OrganizationMapper;
import com.gzu.volunteerblockchain.mapper.UserMapper;
import com.gzu.volunteerblockchain.service.OrganizationService;
import com.gzu.volunteerblockchain.vo.PlatformVOs;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationServiceImpl implements OrganizationService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OrganizationMapper organizationMapper;
    private final OrganizationJoinRequestMapper joinRequestMapper;
    private final UserMapper userMapper;

    public OrganizationServiceImpl(
        OrganizationMapper organizationMapper,
        OrganizationJoinRequestMapper joinRequestMapper,
        UserMapper userMapper
    ) {
        this.organizationMapper = organizationMapper;
        this.joinRequestMapper = joinRequestMapper;
        this.userMapper = userMapper;
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
        Long pendingCount = joinRequestMapper.selectCount(new LambdaQueryWrapper<OrganizationJoinRequest>()
            .eq(OrganizationJoinRequest::getUserId, currentUser.getUserId())
            .eq(OrganizationJoinRequest::getStatus, "pending"));
        if (pendingCount != null && pendingCount > 0) {
            throw new BusinessException("你有待审核的组织申请，请先等待审核结果");
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
                throw new BusinessException("该用户已绑定默认归属组织");
            }
            user.setOrganizationId(joinRequest.getOrganizationId());
            userMapper.updateById(user);
        }

        return toJoinRequestVO(joinRequest, user.getUsername(), requireOrganization(joinRequest.getOrganizationId()).getOrganizationName());
    }

    private PlatformVOs.OrganizationVO toOrganizationVO(Organization organization) {
        PlatformVOs.OrganizationVO vo = new PlatformVOs.OrganizationVO();
        vo.setOrganizationId(organization.getOrganizationId());
        vo.setOrganizationName(organization.getOrganizationName());
        vo.setOrganizationDescription(organization.getOrganizationDescription());
        vo.setAvatarPath(organization.getAvatarPath());
        return vo;
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

    private String format(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_FORMATTER);
    }
}
