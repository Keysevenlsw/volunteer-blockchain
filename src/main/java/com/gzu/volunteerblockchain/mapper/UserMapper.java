package com.gzu.volunteerblockchain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gzu.volunteerblockchain.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("""
        SELECT user_id, username, password_hash, email, avatar_path, organization_id, role, join_date, total_points
        FROM users
        WHERE email = #{email}
        LIMIT 1
        """)
    User selectByEmail(@Param("email") String email);

    @Select("""
        SELECT COUNT(1)
        FROM users
        WHERE email = #{email}
        """)
    Long countByEmail(@Param("email") String email);

    @Update("""
        UPDATE users
        SET total_points = #{totalPoints}
        WHERE user_id = #{userId}
        """)
    int updateTotalPoints(@Param("userId") Integer userId, @Param("totalPoints") int totalPoints);
}
