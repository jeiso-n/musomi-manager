package com.musomi.desktop.api;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.musomi.desktop.model.PageResult;
import com.musomi.desktop.model.UserResponse;

@RequiredArgsConstructor
@Slf4j
public class AdminUserApi {

    private final ApiClient client = ApiClient.getInstance();

    public PageResult<UserResponse> getUsers(int page, int size, String search) {
        String encodedSearch = URLEncoder.encode(search == null ? "" : search, StandardCharsets.UTF_8);
        String path = "/admin/users?page=" + page + "&size=" + size + "&search=" + encodedSearch;
        log.debug("Fetching admin users from {}", path);

        PageResult<?> raw = client.get(path, PageResult.class);
        if (raw == null || raw.data() == null) {
            return new PageResult<>(List.of(), page, size, 0, 0);
        }

        List<UserResponse> users = raw.data().stream()
                .map(this::toUserResponse)
                .toList();
        return new PageResult<>(users, raw.page(), raw.size(), raw.total(), raw.totalPages());
    }

    public void deactivateUser(Long userId) {
        client.delete("/admin/users/" + userId);
    }

    public void createUser(CreateUserRequest request) {
        client.post("/admin/users", request, Void.class);
    }

    public UserResponse createUser(String username, String password, String fullName, String role) {
        return client.post("/admin/users",
                new CreateUserRequest(username, password, fullName, null, null, role),
                UserResponse.class);
    }

    public UserResponse updateUser(Long id, String fullName, String email, String phone) {
        return client.put("/admin/users/" + id,
                new UpdateUserRequest(fullName, email, phone), UserResponse.class);
    }

    private UserResponse toUserResponse(Object value) {
        if (!(value instanceof Map<?, ?> user)) {
            throw new ApiException("INVALID_RESPONSE", "Unexpected user response payload");
        }

        return new UserResponse(
                asLong(user.get("id")),
                asString(user.get("username")),
                asString(user.get("fullName")),
                asString(user.get("role")),
                Boolean.TRUE.equals(user.get("isActive"))
        );
    }

    private static Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private static String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public record CreateUserRequest(String username, String password, String fullName,
                                    String email, String phone, String role) {}

    private record UpdateUserRequest(String fullName, String email, String phone) {}
}
