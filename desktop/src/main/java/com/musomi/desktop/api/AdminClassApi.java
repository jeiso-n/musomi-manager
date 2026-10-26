package com.musomi.desktop.api;

import java.util.Arrays;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.musomi.desktop.model.ClassResponse;

@RequiredArgsConstructor
@Slf4j
public class AdminClassApi {

    private final ApiClient client = ApiClient.getInstance();

    public List<ClassResponse> getClasses() {
        log.debug("Fetching admin classes");
        ClassResponse[] classes = client.get("/admin/classes", ClassResponse[].class);
        return classes == null ? List.of() : Arrays.asList(classes);
    }

    public ClassResponse createClass(
            String name, Long classLevelId, Long academicYearId, Long classTeacherId) {
        return client.post("/admin/classes",
                new CreateClassRequest(name, classLevelId, academicYearId, classTeacherId),
                ClassResponse.class);
    }

    public ClassResponse updateClass(Long id, String name, Long classLevelId,
                                     Long academicYearId, Long classTeacherId) {
        return updateClass(id, new UpdateClassRequest(
                name, classLevelId, academicYearId, classTeacherId));
    }

    public ClassResponse updateClass(Long id, UpdateClassRequest request) {
        return client.put("/admin/classes/" + id,
                request,
                ClassResponse.class);
    }

    public record UpdateClassRequest(
            String name, Long classLevelId, Long academicYearId, Long classTeacherId) {
    }

    private record CreateClassRequest(
            String name, Long classLevelId, Long academicYearId, Long classTeacherId) {
    }
}
