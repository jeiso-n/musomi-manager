package com.musomi.desktop.service;

import com.musomi.desktop.api.AdminClassApi;
import com.musomi.desktop.api.AdminStudentApi;
import com.musomi.desktop.api.AdminSubjectApi;
import com.musomi.desktop.api.AdminTopicApi;
import com.musomi.desktop.api.AdminUserApi;
import com.musomi.desktop.api.TeacherAssessmentApi;
import com.musomi.desktop.api.TeacherClassApi;
import com.musomi.desktop.config.Session;
import com.musomi.desktop.model.AssessmentResponse;
import com.musomi.desktop.model.ClassResponse;
import com.musomi.desktop.model.GlobalSearchResult;
import com.musomi.desktop.model.PageResult;
import com.musomi.desktop.model.StudentResponse;
import com.musomi.desktop.model.SubjectResponse;
import com.musomi.desktop.model.TopicResponse;
import com.musomi.desktop.model.UserResponse;
import com.musomi.desktop.model.dto.TeacherClassResponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Role-aware global search.
 *
 * Administrators use the school-wide admin APIs. Teachers use only endpoints
 * scoped to the authenticated teacher's assignments.
 */
public class GlobalSearchService {

    private final AdminStudentApi studentApi = new AdminStudentApi();
    private final AdminUserApi userApi = new AdminUserApi();
    private final AdminClassApi classApi = new AdminClassApi();
    private final AdminSubjectApi subjectApi = new AdminSubjectApi();
    private final AdminTopicApi topicApi = new AdminTopicApi();

    private final TeacherClassApi teacherClassApi = new TeacherClassApi();
    private final TeacherAssessmentApi teacherAssessmentApi = new TeacherAssessmentApi();

    public List<GlobalSearchResult> search(String searchText) {
        List<GlobalSearchResult> empty = new ArrayList<>();

        if (searchText == null || searchText.isBlank()) {
            return empty;
        }

        String query = searchText.trim().toLowerCase(Locale.ROOT);
        String role = normalizeRole(Session.getInstance().getRole());

        if ("TEACHER".equals(role)) {
            return searchTeacherRecords(query);
        }

        if ("ADMIN".equals(role)) {
            return searchAdminRecords(query);
        }

        return empty;
    }

    private List<GlobalSearchResult> searchAdminRecords(String query) {
        List<GlobalSearchResult> results = new ArrayList<>();

        PageResult<StudentResponse> students =
                studentApi.getStudents(0, 100, "");

        if (students != null && students.data() != null) {
            for (StudentResponse student : students.data()) {
                String title = student.fullName() == null ? "" : student.fullName();
                String description = "Admission number: " + student.admissionNumber()
                        + " | Class: " + student.currentClass();

                addIfMatches(results, query, student.id(), "Student", title, description);
            }
        }

        PageResult<UserResponse> users = userApi.getUsers(0, 100, "");
        if (users != null && users.data() != null) {
            for (UserResponse user : users.data()) {
                String title = user.fullName() == null ? user.username() : user.fullName();
                String description = "Username: " + user.username() + " | Role: " + user.role();

                addIfMatches(results, query, user.id(), "User", title, description);
            }
        }

        List<ClassResponse> classes = classApi.getClasses();
        if (classes != null) {
            for (ClassResponse schoolClass : classes) {
                addIfMatches(results, query, schoolClass.id(), "Class",
                        schoolClass.name(), "Class");
            }
        }

        List<SubjectResponse> subjects = subjectApi.getSubjects();
        if (subjects != null) {
            for (SubjectResponse subject : subjects) {
                addIfMatches(results, query, subject.id(), "Subject",
                        subject.name(), "Subject code: " + subject.code());

                List<TopicResponse> topics = topicApi.getTopics(subject.id());
                if (topics != null) {
                    for (TopicResponse topic : topics) {
                        addIfMatches(results, query, topic.id(), "Topic",
                                topic.name(), "Topic in subject: " + subject.name());
                    }
                }
            }
        }

        return results;
    }

    /**
     * Teacher search is deliberately limited to the authenticated teacher's
     * assignment list, rosters for those assigned class/stream pairs, and
     * assessments returned by the teacher-specific API.
     */
    private List<GlobalSearchResult> searchTeacherRecords(String query) {
        Map<String, GlobalSearchResult> results = new LinkedHashMap<>();

        List<TeacherClassResponse> assignments = teacherClassApi.getMyClasses();
        Set<String> loadedRosters = new LinkedHashSet<>();

        if (assignments != null) {
            for (TeacherClassResponse assignment : assignments) {
                if (assignment == null) {
                    continue;
                }

                String className = safe(assignment.className());
                String streamName = safe(assignment.streamName());
                String classDisplay = streamName.isBlank()
                        ? className : className + " " + streamName;

                addIfMatches(results, query, assignment.classId(), "Class",
                        classDisplay,
                        "Assigned class | Subject: " + safe(assignment.subjectName()));

                addIfMatches(results, query, assignment.subjectId(), "Subject",
                        assignment.subjectName(),
                        "Assigned to: " + classDisplay);

                // The roster endpoint is teacher-scoped and requires both IDs.
                // Avoid calling an unsupported whole-class endpoint when streamId is null.
                if (assignment.classId() != null && assignment.streamId() != null) {
                    String rosterKey = assignment.classId() + ":" + assignment.streamId();

                    if (loadedRosters.add(rosterKey)) {
                        List<StudentResponse> students = teacherClassApi.getMyStudents(
                                assignment.classId(), assignment.streamId());

                        if (students != null) {
                            for (StudentResponse student : students) {
                                if (student == null) {
                                    continue;
                                }

                                String studentName = safe(student.fullName());
                                String description = "Admission number: "
                                        + safe(student.admissionNumber())
                                        + " | Class: " + classDisplay
                                        + " | Subject: " + safe(assignment.subjectName());

                                addTeacherStudentIfMatches(
                                        results, query, student, assignment,
                                        classDisplay, description);
                            }
                        }
                    }
                }
            }
        }

        List<AssessmentResponse> assessments = teacherAssessmentApi.getMyAssessments();
        if (assessments != null) {
            for (AssessmentResponse assessment : assessments) {
                if (assessment == null) {
                    continue;
                }

                String description = "Class: " + safe(assessment.className())
                        + " | Subject: " + safe(assessment.subjectName())
                        + " | Status: " + safe(assessment.status());

                addIfMatches(results, query, assessment.id(), "Assessment",
                        assessment.title(), description);

                // Topic summaries are included with the teacher's own assessments,
                // avoiding administrator-only topic listing endpoints.
                if (assessment.topics() != null) {
                    for (AssessmentResponse.TopicSummary topic : assessment.topics()) {
                        if (topic != null) {
                            addIfMatches(results, query, topic.id(), "Topic",
                                    topic.name(),
                                    "Topic in " + safe(assessment.subjectName())
                                            + " | Assessment: " + safe(assessment.title()));
                        }
                    }
                }
            }
        }

        return new ArrayList<>(results.values());
    }

    private void addIfMatches(
            List<GlobalSearchResult> results,
            String query,
            Long id,
            String category,
            String title,
            String description
    ) {
        if (matches(query, title, description)) {
            results.add(new GlobalSearchResult(
                    id,
                    category,
                    safe(title),
                    safe(description)
            ));
        }
    }

    private void addTeacherStudentIfMatches(
            Map<String, GlobalSearchResult> results,
            String query,
            StudentResponse student,
            TeacherClassResponse assignment,
            String classDisplay,
            String description
    ) {
        String title = safe(student.fullName());
        if (!matches(query, title, description)) {
            return;
        }

        String key = "Student:" + student.id();
        results.putIfAbsent(key, new GlobalSearchResult(
                student.id(),
                "Student",
                title,
                safe(description),
                assignment.classId(),
                assignment.streamId(),
                classDisplay,
                assignment.studentCount()
        ));
    }

    private void addIfMatches(
            Map<String, GlobalSearchResult> results,
            String query,
            Long id,
            String category,
            String title,
            String description
    ) {
        if (!matches(query, title, description)) {
            return;
        }

        String key = category + ":" + id + ":" + safe(title);
        results.putIfAbsent(key, new GlobalSearchResult(
                id,
                category,
                safe(title),
                safe(description)
        ));
    }

    private boolean matches(String query, String title, String description) {
        return (title != null && title.toLowerCase(Locale.ROOT).contains(query))
                || (description != null
                && description.toLowerCase(Locale.ROOT).contains(query));
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized.substring(5) : normalized;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
