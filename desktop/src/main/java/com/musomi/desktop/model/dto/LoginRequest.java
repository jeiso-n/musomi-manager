package com.musomi.desktop.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Authentication request payload for POST /auth/login.
 *
 * @param username user login username
 * @param password user secret password
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LoginRequest(
    @JsonProperty("schoolId")
    Long schoolId,

    @JsonProperty("username")
    String username,

    @JsonProperty("password")
    String password
) {

    public LoginRequest(String username, String password) {
        this(null, username, password);
    }

    public String getUsername() {
        return username;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public String getPassword() {
        return password;
    }
}
