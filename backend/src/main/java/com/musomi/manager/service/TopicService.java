package com.musomi.manager.service;

import java.time.LocalDateTime;
import java.util.List;

import com.musomi.manager.dto.request.CreateTopicRequest;
import com.musomi.manager.dto.response.TopicResponse;
import com.musomi.manager.entity.School;
import com.musomi.manager.entity.Subject;
import com.musomi.manager.entity.Topic;
import com.musomi.manager.exception.ErrorCode;
import com.musomi.manager.exception.ResourceNotFoundException;
import com.musomi.manager.exception.ValidationException;
import com.musomi.manager.mapper.TopicMapper;
import com.musomi.manager.repository.SubjectRepository;
import com.musomi.manager.repository.TopicRepository;
import com.musomi.manager.util.SchoolScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages topics within subjects. */
@Service
@RequiredArgsConstructor
@Slf4j
public class TopicService {

    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;

    /** Lists topics within a subject in configured order. */
    @Transactional(readOnly = true)
    public List<TopicResponse> listTopics(Long schoolId, Long subjectId) {
        Long resolvedSchoolId = SchoolScope.require(schoolId);
        Subject subject = subjectRepository.findById(subjectId)
                .filter(found -> found.getSchool() != null
                        && resolvedSchoolId.equals(found.getSchool().getId()))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED));
        return TopicMapper.toResponseList(
                topicRepository.findBySubjectIdAndSchoolIdOrderBySortOrderAsc(subject.getId(), resolvedSchoolId));
    }

    /** Creates a topic within a school's subject. */
    @Transactional
    public TopicResponse createTopic(Long schoolId, CreateTopicRequest request) {
        Long resolvedSchoolId = SchoolScope.require(schoolId);
        Subject subject = subjectRepository.findById(request.subjectId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED));
        if (subject.getSchool() == null
                || !resolvedSchoolId.equals(subject.getSchool().getId())) {
            throw new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED);
        }
        if (topicRepository.existsBySubjectIdAndName(request.subjectId(), request.name())) {
            throw new ValidationException(ErrorCode.VALIDATION_FAILED);
        }

        Topic topic = Topic.builder()
                .school(School.builder().id(resolvedSchoolId).build())
                .subject(subject)
                .name(request.name())
                .description(request.description())
                .sortOrder(request.sortOrder())
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        Topic saved = topicRepository.save(topic);
        log.info("Topic created: topicId={}, subjectId={}, name={}",
                saved.getId(), request.subjectId(), saved.getName());
        return TopicMapper.toResponse(saved);
    }

    /** Updates a topic within the selected school. */
    @Transactional
    public TopicResponse updateTopic(Long schoolId, Long topicId, CreateTopicRequest request) {
        Long resolvedSchoolId = SchoolScope.require(schoolId);
        Topic topic = topicRepository.findById(topicId)
                .filter(found -> found.getSchool() != null
                        && resolvedSchoolId.equals(found.getSchool().getId()))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED));
        Subject subject = subjectRepository.findById(request.subjectId())
                .filter(found -> found.getSchool() != null
                        && resolvedSchoolId.equals(found.getSchool().getId()))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED));
        topic.setSubject(subject);
        topic.setName(request.name());
        topic.setDescription(request.description());
        topic.setSortOrder(request.sortOrder());
        topic.setUpdatedAt(LocalDateTime.now());
        return TopicMapper.toResponse(topicRepository.save(topic));
    }

    @Transactional
    public void deactivateTopic(Long schoolId, Long topicId) {
        Long resolvedSchoolId = SchoolScope.require(schoolId);
        Topic topic = topicRepository.findById(topicId)
                .filter(found -> found.getSchool() != null
                        && resolvedSchoolId.equals(found.getSchool().getId()))
                .orElseThrow(() -> new ValidationException(ErrorCode.VALIDATION_FAILED));
        topic.setIsActive(false);
        topic.setUpdatedAt(LocalDateTime.now());
        topicRepository.save(topic);
    }
}
