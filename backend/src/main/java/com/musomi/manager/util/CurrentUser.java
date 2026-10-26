package com.musomi.manager.util;

import com.musomi.manager.exception.AuthenticationException;
import com.musomi.manager.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long getUserId() {
        Authentication authentication = getAuthentication();
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }
        throw new AuthenticationException(ErrorCode.SESSION_EXPIRED);
    }

    public static Long getSchoolId() {
        Object details = getAuthentication().getDetails();
        if (details instanceof Long schoolId && schoolId > 0) {
            return schoolId;
        }
        throw new AuthenticationException(ErrorCode.SESSION_EXPIRED);
    }

    public static String getRole() {
        for (GrantedAuthority authority : getAuthentication().getAuthorities()) {
            String value = authority.getAuthority();
            if (value != null && value.startsWith("ROLE_")) {
                return value.substring("ROLE_".length());
            }
        }
        throw new AuthenticationException(ErrorCode.SESSION_EXPIRED);
    }

    private static Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationException(ErrorCode.SESSION_EXPIRED);
        }
        return authentication;
    }
}
