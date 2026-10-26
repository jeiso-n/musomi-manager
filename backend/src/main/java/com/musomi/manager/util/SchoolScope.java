package com.musomi.manager.util;

import com.musomi.manager.exception.AuthenticationException;
import com.musomi.manager.exception.ErrorCode;

/** Validates the school scope supplied by the authenticated request. */
public final class SchoolScope {

    private SchoolScope() {
    }

    public static Long require(Long schoolId) {
        if (schoolId == null || schoolId <= 0) {
            throw new AuthenticationException(ErrorCode.SESSION_EXPIRED);
        }
        return schoolId;
    }
}
