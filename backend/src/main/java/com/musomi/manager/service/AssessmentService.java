package com.musomi.manager.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.musomi.manager.dto.request.CreateAssessmentRequest;
import com.musomi.manager.dto.request.UpdateAssessmentRequest;
import com.musomi.manager.dto.response.AssessmentResponse;
import com.musomi.manager.entity.Assessment;
import com.musomi.manager.entity.AssessmentTopic;
import com.musomi.manager.entity.ClassEntity;
import com.musomi.manager.entity.School;
import com.musomi.manager.entity.Score;
import com.musomi.manager.entity.Stream;
import com.musomi.manager.entity.Student;
import com.musomi.manager.entity.Subject;
import com.musomi.manager.entity.Term;
import com.musomi.manager.entity.Topic;
import com.musomi.manager.entity.User;
import com.musomi.manager.exception.AuthenticationException;
import com.musomi.manager.exception.ErrorCode;
import com.musomi.manager.exception.PermissionDeniedException;
import com.musomi.manager.exception.ResourceNotFoundException;
import com.musomi.manager.exception.ValidationException;
import com.musomi.manager.mapper.AssessmentMapper;
import com.musomi.manager.repository.AssessmentRepository;
import com.musomi.manager.repository.AssessmentTopicRepository;
import com.musomi.manager.repository.ClassRepository;
import com.musomi.manager.repository.ScoreRepository;
import com.musomi.manager.repository.StreamRepository;
import com.musomi.manager.repository.StudentRepository;
import com.musomi.manager.repository.SubjectRepository;
import com.musomi.manager.repository.TermRepository;
import com.musomi.manager.repository.TeacherAssignmentRepository;
import com.musomi.manager.repository.TopicRepository;
import com.musomi.manager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages teacher assessments and publishing. */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentTopicRepository assessmentTopicRepository;
    private final ScoreRepository scoreRepository;
    private final ClassRepository classRepository;
    private final StreamRepository streamRepository;
    private final SubjectRepository subjectRepository;
    private final TermRepository termRepository;
    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final AuditService auditService;

    /** Lists assessments for the teacher with optional filtering. */
    @Transactional(readOnly = true)
    public List<AssessmentResponse> listAssessments(Long schoolId, Long teacherId, Long classId,
                                                   Long subjectId, Long termId, String status) {
        List<Assessment> assessments = teacherId == null
                ? assessmentRepository.findBySchoolId(schoolId)
                : assessmentRepository.findBySchoolIdAndTeacherId(schoolId, teacherId);

        return assessments.stream()
                .filter(assessment -> classId == null || assessment.getClassEntity() != null
                        && classId.equals(assessment.getClassEntity().getId()))
                .filter(assessment -> subjectId == null || assessment.getSubject() != null
                        && subjectId.equals(assessment.getSubject().getId()))
                .filter(assessment -> termId == null || assessment.getTerm() != null
                        && termId.equals(assessment.getTerm().getId()))
                .filter(assessment -> status == null || assessment.getStatus() != null
                        && assessment.getStatus().equalsIgnoreCase(status))
                .map(assessment -> AssessmentMapper.toResponse(
                        assessment, assessmentTopicRepository.findByAssessmentId(assessment.getId())))
                .toList();
    }

    /** Creates a new draft assessment and initializes empty scores for the roster. */
    @Transactional
    public AssessmentResponse createAssessment(Long schoolId, Long teacherId, CreateAssessmentRequest req) {
        if (schoolId == null || schoolId <= 0 || teacherId == null || teacherId <= 0) {
            throw new AuthenticationException(ErrorCode.SESSION_EXPIRED);
        }
        Long resolvedSchoolId = schoolId;

        if (req.topicIds() == null || req.topicIds().isEmpty()) {
            throw new ValidationException(ErrorCode.ASSESSMENT_TOPIC_REQUIRED);
        }

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));

        ClassEntity classEntity = classRepository.findById(req.classId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED));
        if (classEntity.getSchool() == null || !resolvedSchoolId.equals(classEntity.getSchool().getId())) {
            throw new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED);
        }

        Subject subject = subjectRepository.findById(req.subjectId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED));
        if (subject.getSchool() == null || !resolvedSchoolId.equals(subject.getSchool().getId())) {
            throw new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED);
        }

        Term term = termRepository.findById(req.termId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED));
        if (term.getSchool() == null || !resolvedSchoolId.equals(term.getSchool().getId())) {
            throw new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED);
        }
        boolean assigned = teacherAssignmentRepository
                .findBySchoolIdAndTeacherIdOrderByIdAsc(resolvedSchoolId, teacherId)
                .stream()
                .anyMatch(assignment -> assignment.getClassEntity() != null
                        && classEntity.getId().equals(assignment.getClassEntity().getId())
                        && assignment.getSubject() != null
                        && subject.getId().equals(assignment.getSubject().getId())
                        && (assignment.getStream() == null
                        || req.streamId() != null && req.streamId().equals(assignment.getStream().getId()))
                        && (assignment.getAcademicYear() == null || term.getAcademicYear() != null
                        && assignment.getAcademicYear().getId().equals(term.getAcademicYear().getId())));
        if (!assigned) {
            throw new PermissionDeniedException(ErrorCode.NOT_YOUR_CLASS);
        }

        Stream stream = null;
        if (req.streamId() != null) {
            stream = streamRepository.findById(req.streamId())
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED));
            if (stream.getSchool() == null || !resolvedSchoolId.equals(stream.getSchool().getId())
                    || stream.getClassEntity() == null
                    || !classEntity.getId().equals(stream.getClassEntity().getId())) {
                throw new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED);
            }
        }

        Assessment assessment = Assessment.builder()
                .school(School.builder().id(resolvedSchoolId).build())
                .teacher(teacher)
                .classEntity(classEntity)
                .stream(stream)
                .subject(subject)
                .term(term)
                .title(req.title().trim())
                .type(req.type().trim())
                .assessmentDate(req.assessmentDate())
                .maxScore(req.maxScore())
                .status("DRAFT")
                .createdAt(LocalDateTime.now())
                .build();

        Assessment saved = assessmentRepository.save(assessment);

        List<AssessmentTopic> topics = new ArrayList<>();
        for (Long topicId : req.topicIds()) {
            Topic topic = topicRepository.findById(topicId)
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED));
            if (topic.getSchool() == null || !resolvedSchoolId.equals(topic.getSchool().getId())) {
                throw new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED);
            }
            topics.add(AssessmentTopic.builder()
                    .school(School.builder().id(resolvedSchoolId).build())
                    .assessment(saved)
                    .topic(topic)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
        assessmentTopicRepository.saveAll(topics);

        List<Student> roster = studentRepository.findByCurrentClassIdAndIsActiveTrue(classEntity.getId());
        Stream resolvedStream = stream;
        if (resolvedStream != null) {
            roster = roster.stream()
                    .filter(student -> student.getCurrentStream() != null
                            && resolvedStream.getId().equals(student.getCurrentStream().getId()))
                    .toList();
        }

        if (!roster.isEmpty()) {
            List<Score> scoreRows = roster.stream()
                    .map(student -> Score.builder()
                            .school(School.builder().id(resolvedSchoolId).build())
                            .assessment(saved)
                            .student(student)
                            .score(null)
                            .feedback(null)
                            .createdAt(LocalDateTime.now())
                            .build())
                    .toList();
            scoreRepository.saveAll(scoreRows);
        }

        log.info("Assessment created: assessmentId={}, teacherId={}, classId={}",
                saved.getId(), teacherId, classEntity.getId());
        auditService.log("ASSESSMENT_CREATED", "Assessment", saved.getId(), teacherId, resolvedSchoolId);
        return AssessmentMapper.toResponse(saved, assessmentTopicRepository.findByAssessmentId(saved.getId()));
    }

    /** Returns a single assessment by id. */
    @Transactional(readOnly = true)
    public AssessmentResponse getAssessment(Long id, Long schoolId, Long teacherId) {
        Assessment assessment = getOwnedAssessment(id, schoolId, teacherId);
        return AssessmentMapper.toResponse(assessment, assessmentTopicRepository.findByAssessmentId(id));
    }

    /** Updates a draft assessment and replaces its topics. */
    @Transactional
    public AssessmentResponse updateAssessment(Long id, Long schoolId, Long teacherId, UpdateAssessmentRequest req) {
        Assessment assessment = getOwnedAssessment(id, schoolId, teacherId);

        if (!"DRAFT".equalsIgnoreCase(assessment.getStatus())) {
            throw new ValidationException(ErrorCode.ASSESSMENT_ALREADY_PUBLISHED);
        }

        if (req.topicIds() == null || req.topicIds().isEmpty()) {
            throw new ValidationException(ErrorCode.ASSESSMENT_TOPIC_REQUIRED);
        }

        Term term = termRepository.findById(req.termId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED));
        if (term.getSchool() == null || !assessment.getSchool().getId().equals(term.getSchool().getId())) {
            throw new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED);
        }

        Stream stream = null;
        if (req.streamId() != null) {
            stream = streamRepository.findById(req.streamId())
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED));
            if (stream.getSchool() == null || !assessment.getSchool().getId().equals(stream.getSchool().getId())
                    || stream.getClassEntity() == null
                    || !assessment.getClassEntity().getId().equals(stream.getClassEntity().getId())) {
                throw new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED);
            }
        }

        assessment.setStream(stream);
        assessment.setTerm(term);
        assessment.setTitle(req.title().trim());
        assessment.setType(req.type().trim());
        assessment.setAssessmentDate(req.assessmentDate());
        assessment.setMaxScore(req.maxScore());
        assessment.setUpdatedAt(LocalDateTime.now());

        Assessment saved = assessmentRepository.save(assessment);

        assessmentTopicRepository.deleteByAssessmentId(id);
        assessmentTopicRepository.flush();
        List<AssessmentTopic> topics = new ArrayList<>();
        for (Long topicId : req.topicIds()) {
            Topic topic = topicRepository.findById(topicId)
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED));
            if (topic.getSchool() == null || !assessment.getSchool().getId().equals(topic.getSchool().getId())) {
                throw new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED);
            }
            topics.add(AssessmentTopic.builder()
                    .school(assessment.getSchool())
                    .assessment(saved)
                    .topic(topic)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
        assessmentTopicRepository.saveAll(topics);

        log.info("Assessment updated: assessmentId={}, title={}", saved.getId(), saved.getTitle());
        auditService.log("ASSESSMENT_UPDATED", "Assessment", saved.getId(), teacherId, schoolId);
        return AssessmentMapper.toResponse(saved, assessmentTopicRepository.findByAssessmentId(saved.getId()));
    }

    /** Deletes a draft assessment after removing associated topics and scores. */
    @Transactional
    public void deleteAssessment(Long id, Long schoolId, Long teacherId) {
        Assessment assessment = getOwnedAssessment(id, schoolId, teacherId);

        if (!"DRAFT".equalsIgnoreCase(assessment.getStatus())) {
            throw new ValidationException(ErrorCode.ASSESSMENT_ALREADY_PUBLISHED);
        }

        List<Score> scores = scoreRepository.findByAssessmentId(id);
        if (!scores.isEmpty()) {
            scoreRepository.deleteAll(scores);
        }
        assessmentTopicRepository.deleteByAssessmentId(id);
        assessmentRepository.deleteById(id);

        log.info("Assessment deleted: assessmentId={}", id);
        auditService.log("ASSESSMENT_DELETED", "Assessment", id, teacherId, schoolId);
    }

    /** Publishes an assessment after verifying every student has a mark. */
    @Transactional
    public void publishAssessment(Long id, Long schoolId, Long teacherId) {
        Assessment assessment = getOwnedAssessment(id, schoolId, teacherId);

        if (!"DRAFT".equalsIgnoreCase(assessment.getStatus())) {
            throw new ValidationException(ErrorCode.ASSESSMENT_ALREADY_PUBLISHED);
        }

        long emptyScores = scoreRepository.countByAssessmentIdAndScoreIsNull(id);
        if (emptyScores > 0) {
            throw new ValidationException(
                    ErrorCode.CANNOT_PUBLISH_EMPTY,
                    Map.of("count", emptyScores));
        }

        User publisher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));

        assessment.setStatus("PUBLISHED");
        assessment.setPublishedAt(LocalDateTime.now());
        assessment.setPublishedBy(publisher);
        assessment.setUpdatedAt(LocalDateTime.now());
        assessmentRepository.save(assessment);

        log.info("Assessment published: assessmentId={}, publishedBy={} ", id, teacherId);
        auditService.log("ASSESSMENT_PUBLISHED", "Assessment", id, teacherId, schoolId);
    }

    private Assessment getOwnedAssessment(Long id, Long schoolId, Long teacherId) {
        Assessment assessment = assessmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ASSESSMENT_NOT_FOUND));
        if (assessment.getSchool() == null || !schoolId.equals(assessment.getSchool().getId())) {
            throw new ResourceNotFoundException(ErrorCode.ASSESSMENT_NOT_FOUND);
        }
        if (assessment.getTeacher() == null || !teacherId.equals(assessment.getTeacher().getId())) {
            throw new PermissionDeniedException(ErrorCode.NOT_YOUR_CLASS);
        }
        return assessment;
    }
}
