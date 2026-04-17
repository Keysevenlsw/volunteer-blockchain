package com.gzu.volunteerblockchain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gzu.volunteerblockchain.entity.Role;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    @Select("""
        SELECT r.id, r.code, r.name, r.status, r.is_system, r.data_scope, r.sort_order, r.created_at, r.updated_at
        FROM roles r
        JOIN user_roles ur ON ur.role_id = r.id
        WHERE ur.user_id = #{userId}
          AND r.status = 1
        ORDER BY CASE WHEN r.code = #{preferredPrimaryRole} THEN 0 ELSE 1 END, r.sort_order ASC, r.id ASC
        """)
    List<Role> selectActiveRolesByUserId(
        @Param("userId") Integer userId,
        @Param("preferredPrimaryRole") String preferredPrimaryRole
    );
}
