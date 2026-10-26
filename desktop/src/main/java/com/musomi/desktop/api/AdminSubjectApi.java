package com.musomi.desktop.api;

import java.util.Arrays;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.musomi.desktop.model.SubjectResponse;

@RequiredArgsConstructor
@Slf4j
public class AdminSubjectApi {

    private final ApiClient client = ApiClient.getInstance();

    public List<SubjectResponse> getSubjects() {
        log.debug("Fetching admin subjects");
        SubjectResponse[] subjects = client.get("/admin/subjects", SubjectResponse[].class);
        return subjects == null ? List.of() : Arrays.asList(subjects);
    }

    public SubjectResponse createSubject(String code, String name) {
        return createSubject(code, name, true);
    }

    public SubjectResponse createSubject(String code, String name, boolean isActive) {
        return client.post("/admin/subjects", new SubjectRequest(code, name, null, false, isActive),
                SubjectResponse.class);
    }

    public SubjectResponse updateSubject(Long id, String code, String name) {
        return updateSubject(id, code, name, true);
    }

    public SubjectResponse updateSubject(Long id, String code, String name, boolean isActive) {
        return client.put("/admin/subjects/" + id, new SubjectRequest(code, name, null, false, isActive),
                SubjectResponse.class);
    }

    public void deactivateSubject(Long id) {
        client.put("/admin/subjects/" + id + "/deactivate", null, Void.class);
    }

    private record SubjectRequest(String code, String name, String description,
                                  boolean isCore, boolean isActive) {
    }
}
