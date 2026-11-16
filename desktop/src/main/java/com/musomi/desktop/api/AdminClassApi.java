package com.musomi.desktop.api;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

    public List<ClassResponse> getClasses(Long academicYearId) {
        if (academicYearId == null) return getClasses();
        ClassResponse[] classes = client.get(
                "/admin/classes?yearId=" + academicYearId, ClassResponse[].class);
        return classes == null ? List.of() : Arrays.asList(classes);
    }

    public List<ClassLevelResponse> getClassLevels() {
        ClassLevelResponse[] levels = client.get("/admin/class-levels", ClassLevelResponse[].class);
        return levels == null ? List.of() : Arrays.asList(levels);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ClassLevelResponse(
            Long id,
            String name
    ) {}

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
