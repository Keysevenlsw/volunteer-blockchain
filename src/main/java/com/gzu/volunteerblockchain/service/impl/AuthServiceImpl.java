package com.gzu.volunteerblockchain.service.impl;

import com.gzu.volunteerblockchain.common.JwtUtil;
import com.gzu.volunteerblockchain.dto.LoginRequest;
import com.gzu.volunteerblockchain.dto.RegisterRequest;
import com.gzu.volunteerblockchain.entity.JwtToken;
import com.gzu.volunteerblockchain.entity.User;
import com.gzu.volunteerblockchain.exception.BusinessException;
import com.gzu.volunteerblockchain.repository.JwtTokenRepository;
import com.gzu.volunteerblockchain.repository.UserRepository;
import com.gzu.volunteerblockchain.service.AuthService;
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

    private static final String DEFAULT_ROLE = "volunteer";

    private final UserRepository userRepository;
    private final JwtTokenRepository jwtTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(
        UserRepository userRepository,
        JwtTokenRepository jwtTokenRepository,
        PasswordEncoder passwordEncoder,
        JwtUtil jwtUtil
    ) {
        this.userRepository = userRepository;
        this.jwtTokenRepository = jwtTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional
    public UserProfileVO register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException("该邮箱已被注册");
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(DEFAULT_ROLE);
        user.setTotalPoints(0);

        User savedUser = userRepository.save(user);
        return toUserProfile(savedUser);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        User user = userRepository.findByEmail(normalizedEmail)
            .orElseThrow(() -> new BusinessException("邮箱或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("邮箱或密码错误");
        }

        jwtTokenRepository.deleteByUserIdAndExpiresAtBefore(user.getUserId(), LocalDateTime.now());

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(jwtUtil.getExpirationDuration());
        String token = jwtUtil.generateToken(user.getUserId(), user.getEmail(), user.getRole(), issuedAt, expiresAt);

        JwtToken jwtToken = new JwtToken();
        jwtToken.setUserId(user.getUserId());
        jwtToken.setToken(token);
        jwtToken.setCreatedAt(LocalDateTime.now());
        jwtToken.setExpiresAt(LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault()));
        jwtTokenRepository.save(jwtToken);

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

        JwtToken tokenRecord = jwtTokenRepository.findTopByTokenOrderByTokenIdDesc(token)
            .orElseThrow(() -> new BusinessException("登录状态已失效，请重新登录"));

        if (tokenRecord.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("登录令牌已过期，请重新登录");
        }

        Integer userId;
        try {
            userId = Integer.parseInt(claims.getSubject());
        } catch (NumberFormatException ex) {
            throw new BusinessException("Token用户信息不合法");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException("用户不存在"));

        return toUserProfile(user);
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    private UserProfileVO toUserProfile(User user) {
        UserProfileVO userProfileVO = new UserProfileVO();
        userProfileVO.setUserId(user.getUserId());
        userProfileVO.setUsername(user.getUsername());
        userProfileVO.setEmail(user.getEmail());
        userProfileVO.setRole(user.getRole());
        userProfileVO.setTotalPoints(user.getTotalPoints());
        return userProfileVO;
    }
}
