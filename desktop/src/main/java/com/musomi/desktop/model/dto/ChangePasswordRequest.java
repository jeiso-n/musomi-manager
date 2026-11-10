
package com.musomi.desktop.model.dto;

public record ChangePasswordRequest(
        String currentPassword,
        String newPassword
) {
}
