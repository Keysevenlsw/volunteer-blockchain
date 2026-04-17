package com.gzu.volunteerblockchain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gzu.volunteerblockchain.entity.Permission;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    @Select("""
        SELECT DISTINCT p.id, p.code, p.name, p.module, p.http_method, p.api_pattern, p.status, p.created_at, p.updated_at
        FROM permissions p
        JOIN role_permissions rp ON rp.permission_id = p.id
        JOIN user_roles ur ON ur.role_id = rp.role_id
        JOIN roles r ON r.id = ur.role_id
        WHERE ur.user_id = #{userId}
          AND r.status = 1
          AND p.status = 1
        ORDER BY p.module ASC, p.id ASC
        """)
    List<Permission> selectActivePermissionsByUserId(@Param("userId") Integer userId);

    @Select("""
        SELECT DISTINCT p.id, p.code, p.name, p.module, p.http_method, p.api_pattern, p.status, p.created_at, p.updated_at
        FROM permissions p
        JOIN role_permissions rp ON rp.permission_id = p.id
        JOIN roles r ON r.id = rp.role_id
        WHERE r.code = #{roleCode}
          AND r.status = 1
          AND p.status = 1
        ORDER BY p.module ASC, p.id ASC
        """)
    List<Permission> selectActivePermissionsByRoleCode(@Param("roleCode") String roleCode);
}
