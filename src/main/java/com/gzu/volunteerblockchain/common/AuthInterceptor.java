package com.gzu.volunteerblockchain.common;

import com.gzu.volunteerblockchain.entity.JwtToken;
import com.gzu.volunteerblockchain.entity.User;
import com.gzu.volunteerblockchain.exception.BusinessException;
import com.gzu.volunteerblockchain.mapper.JwtTokenMapper;
import com.gzu.volunteerblockchain.mapper.UserMapper;
import com.gzu.volunteerblockchain.service.RbacService;
import io.jsonwebtoken.Claims;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final JwtTokenMapper jwtTokenMapper;
    private final UserMapper userMapper;
    private final RbacService rbacService;

    public AuthInterceptor(
        JwtUtil jwtUtil,
        JwtTokenMapper jwtTokenMapper,
        UserMapper userMapper,
        RbacService rbacService
    ) {
        this.jwtUtil = jwtUtil;
        this.jwtTokenMapper = jwtTokenMapper;
        this.userMapper = userMapper;
        this.rbacService = rbacService;
    }

    @Override
    public boolean preHandle(
        jakarta.servlet.http.HttpServletRequest request,
        jakarta.servlet.http.HttpServletResponse response,
        Object handler
    ) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        String authorizationHeader = request.getHeader("Authorization");
        String token = jwtUtil.extractBearerToken(authorizationHeader);
        Claims claims = jwtUtil.parseToken(token);

        JwtToken tokenRecord = jwtTokenMapper.selectLatestToken(token);
        if (tokenRecord == null || tokenRecord.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Login has expired, please sign in again");
        }

        Integer userId;
        try {
            userId = Integer.parseInt(claims.getSubject());
        } catch (NumberFormatException ex) {
            throw new BusinessException("Invalid token subject");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("User does not exist");
        }

        AuthUser authUser = rbacService.buildAuthUser(user);
        UserContext.set(authUser);

        RoleAllowed roleAllowed = resolveRoleAllowed(handlerMethod);
        if (roleAllowed != null && !matchesAnyRole(authUser, roleAllowed.value())) {
            throw new BusinessException("Current account does not have the required role");
        }

        PermissionAllowed permissionAllowed = resolvePermissionAllowed(handlerMethod);
        if (permissionAllowed != null && !matchesAllPermissions(authUser, permissionAllowed.value())) {
            throw new BusinessException("Current account does not have the required permission");
        }

        return true;
    }

    @Override
    public void afterCompletion(
        jakarta.servlet.http.HttpServletRequest request,
        jakarta.servlet.http.HttpServletResponse response,
        Object handler,
        Exception ex
    ) {
        UserContext.clear();
    }

    private RoleAllowed resolveRoleAllowed(HandlerMethod handlerMethod) {
        RoleAllowed methodAnnotation = handlerMethod.getMethodAnnotation(RoleAllowed.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return handlerMethod.getBeanType().getAnnotation(RoleAllowed.class);
    }

    private PermissionAllowed resolvePermissionAllowed(HandlerMethod handlerMethod) {
        PermissionAllowed methodAnnotation = handlerMethod.getMethodAnnotation(PermissionAllowed.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return handlerMethod.getBeanType().getAnnotation(PermissionAllowed.class);
    }

    private boolean matchesAnyRole(AuthUser authUser, String[] roleCodes) {
        if (roleCodes == null || roleCodes.length == 0) {
            return true;
        }
        for (String roleCode : roleCodes) {
            if (authUser.hasRole(roleCode)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesAllPermissions(AuthUser authUser, String[] permissionCodes) {
        if (permissionCodes == null || permissionCodes.length == 0) {
            return true;
        }
        for (String permissionCode : permissionCodes) {
            if (!authUser.hasPermission(permissionCode)) {
                return false;
            }
        }
        return true;
    }
}
