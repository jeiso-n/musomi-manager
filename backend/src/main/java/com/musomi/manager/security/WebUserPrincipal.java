package com.musomi.manager.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/** Form-login principal carrying the authenticated user's school scope. */
public class WebUserPrincipal extends User {

    private final Long schoolId;

    public WebUserPrincipal(String username, String password,
                            boolean enabled, Collection<? extends GrantedAuthority> authorities,
                            Long schoolId) {
        super(username, password, enabled, true, true, true, authorities);
        this.schoolId = schoolId;
    }

    public Long getSchoolId() {
        return schoolId;
    }
}
