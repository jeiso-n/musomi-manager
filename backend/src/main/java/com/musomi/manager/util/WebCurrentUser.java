package com.musomi.manager.util;

import com.musomi.manager.exception.AuthenticationException;
import com.musomi.manager.exception.ErrorCode;
import com.musomi.manager.security.WebUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Accesses identity and tenant scope for form-authenticated web requests. */
public final class WebCurrentUser {

    private WebCurrentUser() {
    }

    public static Long getSchoolId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof WebUserPrincipal principal)
                || principal.getSchoolId() == null || principal.getSchoolId() <= 0) {
            throw new AuthenticationException(ErrorCode.SESSION_EXPIRED);
        }
        return principal.getSchoolId();
    }
}
