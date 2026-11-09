package com.musomi.desktop.service;

import com.musomi.desktop.api.AdminClassApi;
import com.musomi.desktop.api.AdminStudentApi;
import com.musomi.desktop.api.AdminSubjectApi;
import com.musomi.desktop.api.AdminTopicApi;
import com.musomi.desktop.api.AdminUserApi;
import com.musomi.desktop.model.ClassResponse;
import com.musomi.desktop.model.GlobalSearchResult;
import com.musomi.desktop.model.PageResult;
import com.musomi.desktop.model.StudentResponse;
import com.musomi.desktop.model.SubjectResponse;
import com.musomi.desktop.model.TopicResponse;
import com.musomi.desktop.model.UserResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GlobalSearchService {

    private final AdminStudentApi studentApi = new AdminStudentApi();
    private final AdminUserApi userApi = new AdminUserApi();
    private final AdminClassApi classApi = new AdminClassApi();
    private final AdminSubjectApi subjectApi = new AdminSubjectApi();
    private final AdminTopicApi topicApi = new AdminTopicApi();

    public List<GlobalSearchResult> search(String searchText) {

        List<GlobalSearchResult> results = new ArrayList<>();

        if (searchText == null || searchText.isBlank()) {
            return results;
        }

        String query = searchText.trim().toLowerCase(Locale.ROOT);

        // Search students
        PageResult<StudentResponse> students =
                studentApi.getStudents(0, 100, "");

        if (students != null && students.data() != null) {
            for (StudentResponse student : students.data()) {

                String title = student.fullName() == null
                        ? ""
                        : student.fullName();

                String description =
                        "Admission number: " + student.admissionNumber()
                                + " | Class: " + student.currentClass();

                if (matches(query, title, description)) {
                    results.add(new GlobalSearchResult(
                            student.id(),
                            "Student",
                            title,
                            description
                    ));
                }
            }
        }

        // Search users
        PageResult<UserResponse> users =
                userApi.getUsers(0, 100, "");

        if (users != null && users.data() != null) {
            for (UserResponse user : users.data()) {

                String title = user.fullName() == null
                        ? user.username()
                        : user.fullName();

                String description = "Username: " + user.username()
                        + " | Role: " + user.role();

                if (matches(query, title, description)) {
                    results.add(new GlobalSearchResult(
                            user.id(),
                            "User",
                            title,
                            description
                    ));
                }
            }
        }

        // Search classes
        List<ClassResponse> classes = classApi.getClasses();

        if (classes != null) {
            for (ClassResponse schoolClass : classes) {

                String title = schoolClass.name();
                String description = "Class";

                if (matches(query, title, description)) {
                    results.add(new GlobalSearchResult(
                            schoolClass.id(),
                            "Class",
                            title,
                            description
                    ));
                }
            }
        }

        // Search subjects
        List<SubjectResponse> subjects = subjectApi.getSubjects();

        if (subjects != null) {
            for (SubjectResponse subject : subjects) {

                String title = subject.name();
                String description = "Subject code: " + subject.code();

                if (matches(query, title, description)) {
                    results.add(new GlobalSearchResult(
                            subject.id(),
                            "Subject",
                            title,
                            description
                    ));
                }

                // Search topics belonging to this subject
                List<TopicResponse> topics =
                        topicApi.getTopics(subject.id());

                if (topics != null) {
                    for (TopicResponse topic : topics) {

                        String topicTitle = topic.name();

                        String topicDescription =
                                "Topic in subject: " + subject.name();

                        if (matches(query, topicTitle, topicDescription)) {
                            results.add(new GlobalSearchResult(
                                    topic.id(),
                                    "Topic",
                                    topicTitle,
                                    topicDescription
                            ));
                        }
                    }
                }
            }
        }

        return results;
    }

    private boolean matches(
            String query,
            String title,
            String description
    ) {
        return (title != null
                && title.toLowerCase(Locale.ROOT).contains(query))
                || (description != null
                && description.toLowerCase(Locale.ROOT).contains(query));
    }
}
