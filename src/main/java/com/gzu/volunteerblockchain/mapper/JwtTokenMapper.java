package com.gzu.volunteerblockchain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gzu.volunteerblockchain.entity.JwtToken;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface JwtTokenMapper extends BaseMapper<JwtToken> {

    @Select("""
        SELECT token_id, user_id, token, created_at, expires_at
        FROM jwt_tokens
        WHERE token = #{token}
        ORDER BY token_id DESC
        LIMIT 1
        """)
    JwtToken selectLatestToken(@Param("token") String token);

    @Delete("""
        DELETE FROM jwt_tokens
        WHERE user_id = #{userId}
          AND expires_at < #{now}
        """)
    int deleteExpiredTokens(@Param("userId") Integer userId, @Param("now") LocalDateTime now);
}
