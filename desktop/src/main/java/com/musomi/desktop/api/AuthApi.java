package com.musomi.desktop.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.musomi.desktop.model.dto.ChangePasswordRequest;
import com.musomi.desktop.model.dto.LoginRequest;
import com.musomi.desktop.model.dto.LoginResponse;
import com.musomi.desktop.model.dto.UserResponse;

public class AuthApi {

    private static final Logger log =
            LoggerFactory.getLogger(AuthApi.class);

    private static final String LOGIN_PATH = "/auth/login";
    private static final String ME_PATH = "/auth/me";
    private static final String LOGOUT_PATH = "/auth/logout";
    private static final String CHANGE_PASSWORD_PATH = "/auth/change-password";

    private final ApiClient client;

    public AuthApi() {
        this.client = ApiClient.getInstance();
    }

    public LoginResponse login(LoginRequest request) {
        log.debug("Submitting login request");
        return client.post(LOGIN_PATH, request, LoginResponse.class);
    }

    public UserResponse me() {
        log.debug("Fetching current user profile");
        return client.get(ME_PATH, UserResponse.class);
    }

    public void changePassword(ChangePasswordRequest request) {
        log.debug("Submitting password change request");
        client.post(CHANGE_PASSWORD_PATH, request, Void.class);
    }

    public void logout() {
        log.debug("Sending logout request");
        client.post(LOGOUT_PATH, null, Void.class);
    }
}
