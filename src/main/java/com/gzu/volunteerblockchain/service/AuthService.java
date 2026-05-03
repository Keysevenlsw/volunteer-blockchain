package com.gzu.volunteerblockchain.service;

import com.gzu.volunteerblockchain.dto.LoginRequest;
import com.gzu.volunteerblockchain.dto.PasswordChangeRequest;
import com.gzu.volunteerblockchain.dto.RegisterRequest;
import com.gzu.volunteerblockchain.dto.UserProfileUpdateRequest;
import com.gzu.volunteerblockchain.vo.AuthResponse;
import com.gzu.volunteerblockchain.vo.UserProfileVO;
import org.springframework.web.multipart.MultipartFile;

public interface AuthService {

    UserProfileVO register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserProfileVO getCurrentUser(String authorizationHeader);

    UserProfileVO getCurrentUserProfile(Integer userId);

    UserProfileVO updateCurrentUserProfile(UserProfileUpdateRequest request);

    void changeCurrentUserPassword(PasswordChangeRequest request);

    UserProfileVO updateCurrentUserAvatar(MultipartFile file);
}
