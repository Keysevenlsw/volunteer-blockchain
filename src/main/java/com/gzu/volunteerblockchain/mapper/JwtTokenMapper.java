package com.gzu.volunteerblockchain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gzu.volunteerblockchain.entity.JwtToken;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface JwtTokenMapper extends BaseMapper<JwtToken> {

    JwtToken selectLatestToken(@Param("token") String token);

    int deleteExpiredTokens(@Param("userId") Integer userId, @Param("now") LocalDateTime now);
}
