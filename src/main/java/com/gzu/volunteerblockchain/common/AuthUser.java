package com.gzu.volunteerblockchain.common;

import java.util.LinkedHashSet;
import java.util.List;

public class AuthUser {

    private final Integer userId;
    private final String username;
    private final String email;
    private final String primaryRole;
    private final List<String> roles;
    private final List<String> permissions;
    private final Integer organizationId;

    public AuthUser(
        Integer userId,
        String username,
        String email,
        String primaryRole,
        List<String> roles,
        List<String> permissions,
        Integer organizationId
    ) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.primaryRole = normalizeCode(primaryRole);
        this.roles = normalizeCodes(roles, this.primaryRole);
        this.permissions = normalizeCodes(permissions, null);
        this.organizationId = organizationId;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPrimaryRole() {
        return primaryRole;
    }

    public String getRole() {
        return primaryRole;
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public Integer getOrganizationId() {
        return organizationId;
    }

    public boolean hasRole(String roleCode) {
        String normalizedCode = normalizeCode(roleCode);
        return normalizedCode != null && roles.contains(normalizedCode);
    }

    public boolean hasPermission(String permissionCode) {
        String normalizedCode = normalizeCode(permissionCode);
        return normalizedCode != null && permissions.contains(normalizedCode);
    }

    private static List<String> normalizeCodes(List<String> codes, String preferredFirstCode) {
        LinkedHashSet<String> normalizedCodes = new LinkedHashSet<>();
        if (preferredFirstCode != null) {
            normalizedCodes.add(preferredFirstCode);
        }
        if (codes != null) {
            for (String code : codes) {
                String normalizedCode = normalizeCode(code);
                if (normalizedCode != null) {
                    normalizedCodes.add(normalizedCode);
                }
            }
        }
        return List.copyOf(normalizedCodes);
    }

    private static String normalizeCode(String code) {
        if (code == null) {
            return null;
        }
        String normalizedCode = code.trim();
        return normalizedCode.isEmpty() ? null : normalizedCode;
    }
}
