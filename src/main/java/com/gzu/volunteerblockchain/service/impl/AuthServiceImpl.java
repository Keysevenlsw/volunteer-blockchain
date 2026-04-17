package com.gzu.volunteerblockchain.service.impl;

import com.gzu.volunteerblockchain.common.AuthUser;
import com.gzu.volunteerblockchain.common.JwtUtil;
import com.gzu.volunteerblockchain.common.RoleConstants;
import com.gzu.volunteerblockchain.dto.LoginRequest;
import com.gzu.volunteerblockchain.dto.RegisterRequest;
import com.gzu.volunteerblockchain.entity.JwtToken;
import com.gzu.volunteerblockchain.entity.Organization;
import com.gzu.volunteerblockchain.entity.User;
import com.gzu.volunteerblockchain.exception.BusinessException;
import com.gzu.volunteerblockchain.mapper.JwtTokenMapper;
import com.gzu.volunteerblockchain.mapper.OrganizationMapper;
import com.gzu.volunteerblockchain.mapper.UserMapper;
import com.gzu.volunteerblockchain.service.AuthService;
import com.gzu.volunteerblockchain.service.RbacService;
import com.gzu.volunteerblockchain.vo.AuthResponse;
import com.gzu.volunteerblockchain.vo.UserProfileVO;
import io.jsonwebtoken.Claims;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JwtTokenMapper jwtTokenMapper;
    private final OrganizationMapper organizationMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RbacService rbacService;

    public AuthServiceImpl(
        UserMapper userMapper,
        JwtTokenMapper jwtTokenMapper,
        OrganizationMapper organizationMapper,
        PasswordEncoder passwordEncoder,
        JwtUtil jwtUtil,
        RbacService rbacService
    ) {
        this.userMapper = userMapper;
        this.jwtTokenMapper = jwtTokenMapper;
        this.organizationMapper = organizationMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.rbacService = rbacService;
    }

    @Override
    @Transactional
    public UserProfileVO register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (existsByEmail(email)) {
            throw new BusinessException("该邮箱已被注册");
        }

        User user = new User();
        user.setUsername(trimRequired(request.getUsername(), "用户名不能为空"));
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(resolveRegistrationRole(request.getRole()));
        user.setTotalPoints(0);
        user.setJoinDate(LocalDateTime.now());

        if (RoleConstants.ORGANIZATION_ADMIN.equals(user.getRole())) {
            String organizationName = trimRequired(request.getOrganizationName(), "组织管理员注册时必须填写组织名称");
            Organization organization = new Organization();
            organization.setOrganizationName(organizationName);
            organization.setOrganizationDescription(trimToNull(request.getOrganizationDescription()));
            organization.setCreatedAt(LocalDateTime.now());
            organizationMapper.insert(organization);
            user.setOrganizationId(organization.getOrganizationId());
        }

        userMapper.insert(user);
        rbacService.ensureUserPrimaryRole(user.getUserId(), user.getRole());
        return getCurrentUserProfile(user.getUserId());
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        User user = userMapper.selectByEmail(email);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("邮箱或密码错误");
        }

        jwtTokenMapper.deleteExpiredTokens(user.getUserId(), LocalDateTime.now());

        AuthUser authUser = rbacService.buildAuthUser(user);
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(jwtUtil.getExpirationDuration());
        String token = jwtUtil.generateToken(user.getUserId(), user.getEmail(), authUser.getPrimaryRole(), issuedAt, expiresAt);

        JwtToken jwtToken = new JwtToken();
        jwtToken.setUserId(user.getUserId());
        jwtToken.setToken(token);
        jwtToken.setCreatedAt(LocalDateTime.now());
        jwtToken.setExpiresAt(LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault()));
        jwtTokenMapper.insert(jwtToken);

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setTokenType("Bearer");
        response.setExpiresAt(expiresAt.toEpochMilli());
        response.setUser(toUserProfile(user));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileVO getCurrentUser(String authorizationHeader) {
        String token = jwtUtil.extractBearerToken(authorizationHeader);
        Claims claims = jwtUtil.parseToken(token);

        JwtToken tokenRecord = jwtTokenMapper.selectLatestToken(token);
        if (tokenRecord == null || tokenRecord.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("登录状态已失效，请重新登录");
        }

        Integer userId;
        try {
            userId = Integer.parseInt(claims.getSubject());
        } catch (NumberFormatException ex) {
            throw new BusinessException("无效的登录令牌");
        }

        return getCurrentUserProfile(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileVO getCurrentUserProfile(Integer userId) {
        User user = requireUser(userId);
        return toUserProfile(user);
    }

    private UserProfileVO toUserProfile(User user) {
        AuthUser authUser = rbacService.buildAuthUser(user);
        String organizationName = findOrganizationName(user.getOrganizationId());

        UserProfileVO profile = new UserProfileVO();
        profile.setUserId(user.getUserId());
        profile.setUsername(user.getUsername());
        profile.setEmail(user.getEmail());
        profile.setRole(authUser.getPrimaryRole());
        profile.setPrimaryRole(authUser.getPrimaryRole());
        profile.setRoles(authUser.getRoles());
        profile.setPermissions(authUser.getPermissions());
        profile.setOrganizationId(user.getOrganizationId());
        profile.setOrganizationName(organizationName);
        profile.setTotalPoints(user.getTotalPoints());
        return profile;
    }

    private User requireUser(Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private boolean existsByEmail(String email) {
        Long count = userMapper.countByEmail(email);
        return count != null && count > 0;
    }

    private String findOrganizationName(Integer organizationId) {
        if (organizationId == null) {
            return null;
        }
        Organization organization = organizationMapper.selectById(organizationId);
        return organization == null ? null : organization.getOrganizationName();
    }

    private String resolveRegistrationRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            return RoleConstants.VOLUNTEER;
        }
        String role = rawRole.trim().toLowerCase();
        if (RoleConstants.VOLUNTEER.equals(role) || RoleConstants.ORGANIZATION_ADMIN.equals(role)) {
            return role;
        }
        throw new BusinessException("注册仅允许志愿者或组织管理员角色");
    }

    private String normalizeEmail(String email) {
        return trimRequired(email, "邮箱不能为空").toLowerCase();
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
}
