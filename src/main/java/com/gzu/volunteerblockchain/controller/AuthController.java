package com.gzu.volunteerblockchain.controller;

import com.gzu.volunteerblockchain.dto.LoginRequest;
import com.gzu.volunteerblockchain.dto.PasswordChangeRequest;
import com.gzu.volunteerblockchain.dto.RegisterRequest;
import com.gzu.volunteerblockchain.dto.UserProfileUpdateRequest;
import com.gzu.volunteerblockchain.service.AuthService;
import com.gzu.volunteerblockchain.vo.ApiResponse;
import com.gzu.volunteerblockchain.vo.AuthResponse;
import com.gzu.volunteerblockchain.vo.UserProfileVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<UserProfileVO> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("注册成功", authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("登录成功", authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileVO> me(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        return ApiResponse.success(authService.getCurrentUser(authorizationHeader));
    }

    @PutMapping("/me")
    public ApiResponse<UserProfileVO> updateMe(@Valid @RequestBody UserProfileUpdateRequest request) {
        return ApiResponse.success("个人信息已更新", authService.updateCurrentUserProfile(request));
    }

    @PostMapping("/me/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        authService.changeCurrentUserPassword(request);
        return ApiResponse.success("密码已修改", null);
    }

    @PostMapping("/me/avatar")
    public ApiResponse<UserProfileVO> updateAvatar(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success("头像已更新", authService.updateCurrentUserAvatar(file));
    }
}
