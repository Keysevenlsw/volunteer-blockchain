package com.gzu.volunteerblockchain.common;

import com.gzu.volunteerblockchain.exception.BusinessException;

public final class UserContext {

    private static final ThreadLocal<AuthUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(AuthUser authUser) {
        HOLDER.set(authUser);
    }

    public static AuthUser getRequiredUser() {
        AuthUser authUser = HOLDER.get();
        if (authUser == null) {
            throw new BusinessException("未登录或登录状态已失效");
        }
        return authUser;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
