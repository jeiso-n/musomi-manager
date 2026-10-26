package com.musomi.manager.service;

import java.time.LocalDateTime;
import java.util.List;

import com.musomi.manager.dto.request.CreateSubjectRequest;
import com.musomi.manager.dto.response.SubjectResponse;
import com.musomi.manager.entity.School;
import com.musomi.manager.entity.Subject;
import com.musomi.manager.exception.ErrorCode;
import com.musomi.manager.exception.ValidationException;
import com.musomi.manager.mapper.SubjectMapper;
import com.musomi.manager.repository.SubjectRepository;
import com.musomi.manager.util.SchoolScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Manages subjects offered by a school. */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final AuditService auditService;

    /** Lists active subjects for a school in name order. */
    @Transactional(readOnly = true)
    public List<SubjectResponse> listSubjects(Long schoolId) {
        Long resolvedSchoolId = resolveSchoolId(schoolId);
        return SubjectMapper.toResponseList(
                subjectRepository.findBySchoolIdAndIsActiveTrueOrderByNameAsc(resolvedSchoolId));
    }

    /** Creates a subject for a school. */
    @Transactional
    public SubjectResponse createSubject(Long schoolId, CreateSubjectRequest request) {
        Long resolvedSchoolId = resolveSchoolId(schoolId);
        if (subjectRepository.existsBySchoolIdAndCode(resolvedSchoolId, request.code())) {
            throw new ValidationException(ErrorCode.VALIDATION_FAILED);
        }

        Subject subject = Subject.builder()
                .school(School.builder().id(resolvedSchoolId).build())
                .code(request.code())
                .name(request.name())
                .description(request.description())
                .isCore(Boolean.TRUE.equals(request.isCore()))
                .isActive(request.isActive() == null || request.isActive())
                .createdAt(LocalDateTime.now())
                .build();
        Subject saved = subjectRepository.save(subject);
        log.info("Subject created: subjectId={}, code={}", saved.getId(), saved.getCode());
        if (auditService != null) {
            auditService.log("SUBJECT_CREATED", "Subject", saved.getId(),
                    currentUserId(), resolvedSchoolId);
        }
        return SubjectMapper.toResponse(saved);
    }

    /** Updates a subject within the selected school. */
    @Transactional
    public SubjectResponse updateSubject(Long schoolId, Long subjectId, CreateSubjectRequest request) {
        Long resolvedSchoolId = resolveSchoolId(schoolId);
        Subject subject = subjectRepository.findById(subjectId)
                .filter(found -> found.getSchool() != null
                        && resolvedSchoolId.equals(found.getSchool().getId()))
                .orElseThrow(() -> new ValidationException(ErrorCode.VALIDATION_FAILED));
        if (!subject.getCode().equals(request.code())
                && subjectRepository.existsBySchoolIdAndCode(resolvedSchoolId, request.code())) {
            throw new ValidationException(ErrorCode.VALIDATION_FAILED);
        }
        subject.setCode(request.code());
        subject.setName(request.name());
        subject.setDescription(request.description());
        subject.setIsCore(Boolean.TRUE.equals(request.isCore()));
        if (request.isActive() != null) {
            subject.setIsActive(request.isActive());
        }
        subject.setUpdatedAt(LocalDateTime.now());
        return SubjectMapper.toResponse(subjectRepository.save(subject));
    }

    @Transactional
    public void deactivateSubject(Long schoolId, Long subjectId) {
        Long resolvedSchoolId = resolveSchoolId(schoolId);
        Subject subject = subjectRepository.findById(subjectId)
                .filter(found -> found.getSchool() != null
                        && resolvedSchoolId.equals(found.getSchool().getId()))
                .orElseThrow(() -> new ValidationException(ErrorCode.VALIDATION_FAILED));
        subject.setIsActive(false);
        subject.setUpdatedAt(LocalDateTime.now());
        subjectRepository.save(subject);
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof Long userId
                ? userId : null;
    }

    private Long resolveSchoolId(Long schoolId) {
        return SchoolScope.require(schoolId);
    }
}
