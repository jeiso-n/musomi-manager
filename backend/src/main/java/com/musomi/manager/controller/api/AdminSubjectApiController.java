package com.musomi.manager.controller.api;

import java.util.List;

import com.musomi.manager.dto.request.CreateSubjectRequest;
import com.musomi.manager.dto.request.CreateTopicRequest;
import com.musomi.manager.dto.response.ApiResponse;
import com.musomi.manager.dto.response.SubjectResponse;
import com.musomi.manager.dto.response.TopicResponse;
import com.musomi.manager.service.SubjectService;
import com.musomi.manager.service.TopicService;
import com.musomi.manager.util.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin endpoints for subjects and their topics. */
@RestController
@RequestMapping("/api/v1/admin/subjects")
@RequiredArgsConstructor
public class AdminSubjectApiController {

    private final SubjectService subjectService;
    private final TopicService topicService;

    /** Lists active subjects for the school. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<List<SubjectResponse>> listSubjects() {
        return ApiResponse.success(subjectService.listSubjects(CurrentUser.getSchoolId()));
    }

    /** Creates a subject. */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SubjectResponse>> createSubject(
            @Valid @RequestBody CreateSubjectRequest request) {
        SubjectResponse created = subjectService.createSubject(CurrentUser.getSchoolId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SubjectResponse> updateSubject(
            @PathVariable Long id,
            @Valid @RequestBody CreateSubjectRequest request) {
        return ApiResponse.success(subjectService.updateSubject(
                CurrentUser.getSchoolId(), id, request));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateSubject(@PathVariable Long id) {
        subjectService.deactivateSubject(CurrentUser.getSchoolId(), id);
        return ResponseEntity.noContent().build();
    }

    /** Lists topics belonging to a subject. */
    @GetMapping("/{id}/topics")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<List<TopicResponse>> listTopics(@PathVariable Long id) {
        return ApiResponse.success(topicService.listTopics(CurrentUser.getSchoolId(), id));
    }

    /** Creates a topic within the specified subject. */
    @PostMapping("/{id}/topics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TopicResponse>> createTopic(
            @PathVariable Long id,
            @Valid @RequestBody CreateTopicRequest request) {
        CreateTopicRequest topicRequest = new CreateTopicRequest(
                id, request.name(), request.description(), request.sortOrder());
        TopicResponse created = topicService.createTopic(CurrentUser.getSchoolId(), topicRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @PutMapping("/{id}/topics/{topicId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TopicResponse> updateTopic(
            @PathVariable Long id,
            @PathVariable Long topicId,
            @Valid @RequestBody CreateTopicRequest request) {
        CreateTopicRequest topicRequest = new CreateTopicRequest(
                id, request.name(), request.description(), request.sortOrder());
        return ApiResponse.success(topicService.updateTopic(
                CurrentUser.getSchoolId(), topicId, topicRequest));
    }
}
