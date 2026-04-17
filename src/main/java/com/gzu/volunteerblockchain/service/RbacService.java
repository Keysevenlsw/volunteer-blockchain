package com.gzu.volunteerblockchain.service;

import com.gzu.volunteerblockchain.common.AuthUser;
import com.gzu.volunteerblockchain.entity.Permission;
import com.gzu.volunteerblockchain.entity.Role;
import com.gzu.volunteerblockchain.entity.User;
import java.util.List;

public interface RbacService {

    AuthUser buildAuthUser(User user);

    List<String> getUserRoleCodes(Integer userId, String preferredPrimaryRole);

    List<String> getUserPermissionCodes(Integer userId);

    void ensureUserPrimaryRole(Integer userId, String roleCode);

    void replaceUserRoles(Integer userId, List<String> roleCodes);

    List<Role> listRoles();

    List<Permission> listPermissions();

    List<String> getRolePermissionCodes(String roleCode);
}
