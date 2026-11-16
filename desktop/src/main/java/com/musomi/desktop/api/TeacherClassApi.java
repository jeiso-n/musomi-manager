package com.musomi.desktop.api;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.musomi.desktop.model.StudentResponse;
import com.musomi.desktop.model.dto.TeacherClassResponse;

/**
 * API client for teacher-facing class endpoints.
 *
 * Uses endpoints that restrict data to the currently authenticated teacher.
 */
public class TeacherClassApi {

    private static final Logger log = LoggerFactory.getLogger(TeacherClassApi.class);

    private static final String CLASSES_PATH = "/teacher/classes";

    private final ApiClient client;

    public TeacherClassApi() {
        this.client = ApiClient.getInstance();
    }

    /**
     * Returns class, stream and subject assignments belonging to the logged-in teacher.
     */
    public List<TeacherClassResponse> getMyClasses() {
        log.debug("Fetching teacher classes from {}", CLASSES_PATH);
        TeacherClassResponse[] array =
                client.get(CLASSES_PATH, TeacherClassResponse[].class);
        return array == null ? List.of() : Arrays.asList(array);
    }

    /**
     * Returns active students in one class stream assigned to the logged-in teacher.
     * The backend verifies the teacher's assignment before returning the roster.
     */
    public List<StudentResponse> getMyStudents(Long classId, Long streamId) {
        if (classId == null || streamId == null) {
            return List.of();
        }

        String path = CLASSES_PATH + "/" + classId
                + "/streams/" + streamId + "/students";

        log.debug("Fetching assigned class roster from {}", path);
        StudentResponse[] array = client.get(path, StudentResponse[].class);
        return array == null ? List.of() : Arrays.asList(array);
    }
}
