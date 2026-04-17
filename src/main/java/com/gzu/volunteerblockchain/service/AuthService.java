package com.gzu.volunteerblockchain.service;

import com.gzu.volunteerblockchain.dto.LoginRequest;
import com.gzu.volunteerblockchain.dto.RegisterRequest;
import com.gzu.volunteerblockchain.vo.AuthResponse;
import com.gzu.volunteerblockchain.vo.UserProfileVO;

public interface AuthService {

    UserProfileVO register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserProfileVO getCurrentUser(String authorizationHeader);

    UserProfileVO getCurrentUserProfile(Integer userId);
}
