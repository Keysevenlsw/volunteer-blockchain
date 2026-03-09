package com.gzu.volunteerblockchain.vo;

public class AuthResponse {

    private String token;
    private String tokenType;
    private long expiresAt;
    private UserProfileVO user;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public UserProfileVO getUser() {
        return user;
    }

    public void setUser(UserProfileVO user) {
        this.user = user;
    }
}
