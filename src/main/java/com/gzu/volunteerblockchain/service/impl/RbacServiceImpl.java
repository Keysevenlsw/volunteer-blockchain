package com.gzu.volunteerblockchain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gzu.volunteerblockchain.common.AuthUser;
import com.gzu.volunteerblockchain.entity.Permission;
import com.gzu.volunteerblockchain.entity.Role;
import com.gzu.volunteerblockchain.entity.User;
import com.gzu.volunteerblockchain.entity.UserRole;
import com.gzu.volunteerblockchain.exception.BusinessException;
import com.gzu.volunteerblockchain.mapper.PermissionMapper;
import com.gzu.volunteerblockchain.mapper.RoleMapper;
import com.gzu.volunteerblockchain.mapper.UserMapper;
import com.gzu.volunteerblockchain.mapper.UserRoleMapper;
import com.gzu.volunteerblockchain.service.RbacService;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RbacServiceImpl implements RbacService {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserMapper userMapper;

    public RbacServiceImpl(
        RoleMapper roleMapper,
        PermissionMapper permissionMapper,
        UserRoleMapper userRoleMapper,
        UserMapper userMapper
    ) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.userRoleMapper = userRoleMapper;
        this.userMapper = userMapper;
    }

    @Override
    public AuthUser buildAuthUser(User user) {
        List<String> roleCodes = getUserRoleCodes(user.getUserId(), user.getRole());
        if (roleCodes.isEmpty() && !isBlank(user.getRole())) {
            roleCodes = List.of(user.getRole());
        }
        String primaryRole = resolvePrimaryRole(user.getRole(), roleCodes);
        List<String> permissionCodes = getUserPermissionCodes(user.getUserId());
        return new AuthUser(
            user.getUserId(),
            user.getUsername(),
            user.getEmail(),
            primaryRole,
            roleCodes,
            permissionCodes,
            user.getOrganizationId()
        );
    }

    @Override
    public List<String> getUserRoleCodes(Integer userId, String preferredPrimaryRole) {
        if (userId == null) {
            return List.of();
        }
        return roleMapper.selectActiveRolesByUserId(userId, preferredPrimaryRole).stream()
            .map(Role::getCode)
            .filter(code -> !isBlank(code))
            .distinct()
            .toList();
    }

    @Override
    public List<String> getUserPermissionCodes(Integer userId) {
        if (userId == null) {
            return List.of();
        }
        return permissionMapper.selectActivePermissionsByUserId(userId).stream()
            .map(Permission::getCode)
            .filter(code -> !isBlank(code))
            .distinct()
            .toList();
    }

    @Override
    @Transactional
    public void ensureUserPrimaryRole(Integer userId, String roleCode) {
        User user = requireUser(userId);
        Role role = requireActiveRole(roleCode);
        ensureUserRoleMapping(userId, role.getId());
        if (!Objects.equals(user.getRole(), role.getCode())) {
            user.setRole(role.getCode());
            userMapper.updateById(user);
        }
    }

    @Override
    @Transactional
    public void replaceUserRoles(Integer userId, List<String> roleCodes) {
        User user = requireUser(userId);
        LinkedHashSet<String> normalizedRoleCodes = roleCodes == null ? new LinkedHashSet<>() : roleCodes.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(code -> !code.isEmpty())
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalizedRoleCodes.isEmpty()) {
            throw new BusinessException("至少需要保留一个角色");
        }

        List<Role> roles = roleMapper.selectList(new LambdaQueryWrapper<Role>()
                .in(Role::getCode, normalizedRoleCodes)
                .eq(Role::getStatus, 1))
            .stream()
            .collect(Collectors.collectingAndThen(
                Collectors.toMap(Role::getCode, role -> role, (left, right) -> left),
                roleMap -> normalizedRoleCodes.stream()
                    .map(roleMap::get)
                    .filter(Objects::nonNull)
                    .toList()
            ));

        if (roles.size() != normalizedRoleCodes.size()) {
            throw new BusinessException("包含不存在或已停用的角色");
        }

        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        LocalDateTime now = LocalDateTime.now();
        for (Role role : roles) {
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(role.getId());
            userRole.setCreatedAt(now);
            userRoleMapper.insert(userRole);
        }

        String primaryRole = roles.get(0).getCode();
        if (!Objects.equals(primaryRole, user.getRole())) {
            user.setRole(primaryRole);
            userMapper.updateById(user);
        }
    }

    @Override
    public List<Role> listRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<Role>()
            .orderByAsc(Role::getSortOrder)
            .orderByAsc(Role::getId));
    }

    @Override
    public List<Permission> listPermissions() {
        return permissionMapper.selectList(new LambdaQueryWrapper<Permission>()
            .orderByAsc(Permission::getModule)
            .orderByAsc(Permission::getId));
    }

    @Override
    public List<String> getRolePermissionCodes(String roleCode) {
        Role role = requireActiveRole(roleCode);
        return permissionMapper.selectActivePermissionsByRoleCode(role.getCode()).stream()
            .map(Permission::getCode)
            .filter(code -> !isBlank(code))
            .distinct()
            .toList();
    }

    private User requireUser(Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private Role requireActiveRole(String roleCode) {
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
            .eq(Role::getCode, roleCode)
            .last("LIMIT 1"));
        if (role == null || role.getStatus() == null || role.getStatus() != 1) {
            throw new BusinessException("角色不存在或已停用: " + roleCode);
        }
        return role;
    }

    private void ensureUserRoleMapping(Integer userId, Integer roleId) {
        Long exists = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>()
            .eq(UserRole::getUserId, userId)
            .eq(UserRole::getRoleId, roleId));
        if (exists != null && exists > 0) {
            return;
        }
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        userRole.setCreatedAt(LocalDateTime.now());
        userRoleMapper.insert(userRole);
    }

    private String resolvePrimaryRole(String currentRole, List<String> roleCodes) {
        if (!isBlank(currentRole) && roleCodes.contains(currentRole)) {
            return currentRole;
        }
        if (!roleCodes.isEmpty()) {
            return roleCodes.get(0);
        }
        return currentRole;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
