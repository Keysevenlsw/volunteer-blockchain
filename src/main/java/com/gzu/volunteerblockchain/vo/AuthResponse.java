package com.gzu.volunteerblockchain.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AuthResponse {

    private String token;
    private String tokenType;
    private long expiresAt;
    private UserProfileVO user;
}
