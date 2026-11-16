package com.musomi.desktop.api;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Authenticated API operations for managing streams within a class. */
public final class AdminStreamApi {
    private final ApiClient client = ApiClient.getInstance();

    public List<StreamResponse> getStreams(Long classId) {
        if (classId == null) return List.of();
        StreamResponse[] result = client.get("/admin/streams?classId=" + classId, StreamResponse[].class);
        return result == null ? List.of() : Arrays.asList(result);
    }

    public StreamResponse createStream(Long classId, String name) {
        return client.post("/admin/streams", new CreateStreamRequest(classId, name, null), StreamResponse.class);
    }

    public record StreamResponse(Long id, Long classId, String name, Integer capacity, Boolean isActive) {
        @Override public String toString() { return name == null ? "" : name; }
    }
    private record CreateStreamRequest(Long classId, String name, Integer capacity) {}
}
