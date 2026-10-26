package com.musomi.desktop.api;

import java.util.Arrays;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.musomi.desktop.model.AssessmentResponse;
import com.musomi.desktop.model.ClassResponse;
import com.musomi.desktop.model.CreateAssessmentRequest;
import com.musomi.desktop.model.SubjectResponse;
import com.musomi.desktop.model.TermResponse;
import com.musomi.desktop.model.TopicResponse;

@RequiredArgsConstructor
@Slf4j
public class TeacherAssessmentApi {

    private final ApiClient client = ApiClient.getInstance();

    public List<ClassResponse> getMyClasses() {
        ClassResponse[] classes = client.get("/admin/classes", ClassResponse[].class);
        return classes == null ? List.of() : Arrays.asList(classes);
    }

    public List<SubjectResponse> getSubjects() {
        SubjectResponse[] subjects = client.get("/admin/subjects", SubjectResponse[].class);
        return subjects == null ? List.of() : Arrays.asList(subjects);
    }

    public List<TermResponse> getTerms() {
        TermResponse[] terms = client.get("/admin/terms", TermResponse[].class);
        return terms == null ? List.of() : Arrays.asList(terms);
    }

    public List<TopicResponse> getTopics(Long subjectId) {
        TopicResponse[] topics = client.get(
                "/admin/subjects/" + subjectId + "/topics",
                TopicResponse[].class);
        return topics == null ? List.of() : Arrays.asList(topics);
    }

    public AssessmentResponse createAssessment(CreateAssessmentRequest request) {
        return client.post("/teacher/assessments", request, AssessmentResponse.class);
    }

    public List<AssessmentResponse> getMyAssessments() {
        AssessmentResponse[] assessments = client.get(
                "/teacher/assessments", AssessmentResponse[].class);
        return assessments == null ? List.of() : Arrays.asList(assessments);
    }
}
