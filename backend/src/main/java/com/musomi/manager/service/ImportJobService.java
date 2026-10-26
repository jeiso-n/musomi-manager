package com.musomi.manager.service;

import java.time.LocalDateTime;
import java.util.List;

import com.musomi.manager.dto.response.ImportJobResponse;
import com.musomi.manager.entity.ImportJob;
import com.musomi.manager.entity.School;
import com.musomi.manager.entity.User;
import com.musomi.manager.exception.ErrorCode;
import com.musomi.manager.exception.ResourceNotFoundException;
import com.musomi.manager.mapper.ImportJobMapper;
import com.musomi.manager.repository.ImportJobRepository;
import com.musomi.manager.util.SchoolScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages import job records. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImportJobService {

    private final ImportJobRepository importJobRepository;

    /** Lists import jobs for a school. */
    @Transactional(readOnly = true)
    public List<ImportJobResponse> listBySchool(Long schoolId) {
        return ImportJobMapper.toResponseList(
                importJobRepository.findBySchoolIdOrderByCreatedAtDesc(SchoolScope.require(schoolId)));
    }

    /** Returns the import job with the specified reference. */
    @Transactional(readOnly = true)
    public ImportJobResponse getByReference(String reference, Long schoolId) {
        ImportJob job = importJobRepository.findByJobReferenceAndSchoolId(
                        reference, SchoolScope.require(schoolId))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED));
        return ImportJobMapper.toResponse(job);
    }

    /** Creates a pending import job with empty validation counts. */
    @Transactional
    public ImportJobResponse create(Long schoolId, String jobReference, String importType, Integer totalRows,
                                   String originalFilename, Long createdBy) {
        ImportJob job = ImportJob.builder()
                .school(School.builder().id(SchoolScope.require(schoolId)).build())
                .jobReference(jobReference)
                .importType(importType)
                .status("PENDING")
                .totalRows(totalRows)
                .validRows(0)
                .errorRows(0)
                .originalFilename(originalFilename)
                .createdBy(User.builder().id(createdBy).build())
                .createdAt(LocalDateTime.now())
                .build();
        ImportJob saved = importJobRepository.save(job);
        log.info("Import job created: reference={}, schoolId={}", jobReference, schoolId);
        return ImportJobMapper.toResponse(saved);
    }
}
