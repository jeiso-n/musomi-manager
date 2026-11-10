package com.musomi.desktop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.musomi.desktop.api.AuthApi;
import com.musomi.desktop.api.ApiException;
import com.musomi.desktop.config.AppConfig;
import com.musomi.desktop.config.Session;
import com.musomi.desktop.model.dto.ChangePasswordRequest;
import com.musomi.desktop.model.dto.LoginRequest;
import com.musomi.desktop.model.dto.LoginResponse;
import com.musomi.desktop.model.dto.UserResponse;

/**
 * Client-side service for authentication operations.
 *
 * Controllers must never call AuthApi directly.
 * All authentication operations go through this service.
 */
public class AuthService {

    private static final Logger log =
            LoggerFactory.getLogger(AuthService.class);

    private final AuthApi api;
    private final Session session;

    public AuthService(AuthApi api, Session session) {
        this.api = api;
        this.session = session;
    }

    public AuthService() {
        this(new AuthApi(), Session.getInstance());
    }

    public LoginResponse login(String username, String password) {
        log.debug("AuthService.login");

        LoginResponse response = api.login(new LoginRequest(
                AppConfig.getInstance().getSchoolId(),
                username,
                password
        ));

        if (response == null) {
            throw new ApiException(
                    "INVALID_AUTH_RESPONSE",
                    "The server returned an invalid login response."
            );
        }

        try {
            session.set(
                    response.getToken(),
                    response.getExpiresAt(),
                    response.getUser()
            );
        } catch (IllegalArgumentException ex) {
            throw new ApiException(
                    "INVALID_AUTH_RESPONSE",
                    "The server returned an invalid login response."
            );
        }

        log.debug("Session established");
        return response;
    }

    public void changePassword(String currentPassword, String newPassword) {
        log.debug("AuthService.changePassword");

        api.changePassword(
                new ChangePasswordRequest(currentPassword, newPassword)
        );
    }

    public void logout() {
        log.debug("AuthService.logout");

        try {
            api.logout();
        } finally {
            session.clear();
            log.debug("Session cleared after logout");
        }
    }

    public UserResponse me() {
        log.debug("AuthService.me");
        return api.me();
    }
}
