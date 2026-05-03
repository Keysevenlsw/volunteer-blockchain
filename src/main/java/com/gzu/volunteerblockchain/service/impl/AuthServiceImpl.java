package com.gzu.volunteerblockchain.service.impl;

import com.gzu.volunteerblockchain.common.AuthUser;
import com.gzu.volunteerblockchain.common.JwtUtil;
import com.gzu.volunteerblockchain.common.RoleConstants;
import com.gzu.volunteerblockchain.common.UserContext;
import com.gzu.volunteerblockchain.dto.LoginRequest;
import com.gzu.volunteerblockchain.dto.PasswordChangeRequest;
import com.gzu.volunteerblockchain.dto.RegisterRequest;
import com.gzu.volunteerblockchain.dto.UserProfileUpdateRequest;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AuthServiceImpl implements AuthService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserMapper userMapper;
    private final JwtTokenMapper jwtTokenMapper;
    private final OrganizationMapper organizationMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RbacService rbacService;

    @Value("${storage.upload-dir:src/main/resources/static/uploads}")
    private String uploadDir;

    @Value("${storage.avatar-dir:src/main/resources/static/uploads/avatars}")
    private String avatarDir;

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

    @Override
    @Transactional
    public UserProfileVO updateCurrentUserProfile(UserProfileUpdateRequest request) {
        Integer userId = UserContext.getRequiredUser().getUserId();
        User user = requireUser(userId);

        String username = trimToNull(request.getUsername());
        if (username != null) {
            user.setUsername(username);
        }

        String email = trimToNull(request.getEmail());
        if (email != null) {
            email = normalizeEmail(email);
            Long sameEmailCount = userMapper.countByEmailExcludingUserId(email, userId);
            if (sameEmailCount != null && sameEmailCount > 0) {
                throw new BusinessException("该邮箱已被其他用户使用");
            }
            user.setEmail(email);
        }

        userMapper.updateById(user);
        return getCurrentUserProfile(userId);
    }

    @Override
    @Transactional
    public void changeCurrentUserPassword(PasswordChangeRequest request) {
        Integer userId = UserContext.getRequiredUser().getUserId();
        User user = requireUser(userId);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException("原密码不正确");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public UserProfileVO updateCurrentUserAvatar(MultipartFile file) {
        String contentType = file == null ? null : file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new BusinessException("头像文件必须是图片");
        }

        Integer userId = UserContext.getRequiredUser().getUserId();
        User user = requireUser(userId);
        String oldAvatarPath = user.getAvatarPath();
        String newAvatarPath = storeAvatar(file);
        try {
            user.setAvatarPath(newAvatarPath);
            userMapper.updateById(user);
        } catch (RuntimeException ex) {
            deleteUploadedFile(newAvatarPath);
            throw ex;
        }
        deleteUploadedFile(oldAvatarPath);
        return getCurrentUserProfile(userId);
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
        profile.setAvatarPath(user.getAvatarPath());
        profile.setJoinDate(formatDateTime(user.getJoinDate()));
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

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_FORMATTER);
    }

    private String storeAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传头像不能为空");
        }

        String extension = resolveImageExtension(file);
        String filename = UUID.randomUUID().toString().replace("-", "") + extension;
        Path basePath = Paths.get(avatarDir).toAbsolutePath().normalize();
        Path targetFile = basePath.resolve(filename).normalize();

        if (!targetFile.startsWith(basePath)) {
            throw new BusinessException("非法头像文件路径");
        }

        try {
            Files.createDirectories(basePath);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BusinessException("头像保存失败: " + ex.getMessage());
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
            // 删除旧头像失败不影响新头像生效。
        }
    }
}
