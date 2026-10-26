package com.musomi.manager.util;

import com.musomi.manager.exception.AuthenticationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsUserAndSchoolFromAuthentication() {
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        42L, null, List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));
        authentication.setDetails(7L);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(CurrentUser.getUserId()).isEqualTo(42L);
        assertThat(CurrentUser.getSchoolId()).isEqualTo(7L);
    }

    @Test
    void rejectsAuthenticationWithoutNumericUserId() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "teacher", null, List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))));

        assertThatThrownBy(CurrentUser::getUserId).isInstanceOf(AuthenticationException.class);
    }

    @Test
    void rejectsAuthenticationWithoutSchoolId() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        42L, null, List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))));

        assertThatThrownBy(CurrentUser::getSchoolId).isInstanceOf(AuthenticationException.class);
    }
}
