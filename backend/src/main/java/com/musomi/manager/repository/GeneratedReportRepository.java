package com.musomi.manager.repository;

import java.util.List;
import java.util.Optional;

import com.musomi.manager.entity.GeneratedReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneratedReportRepository extends JpaRepository<GeneratedReport, Long> {

    Optional<GeneratedReport> findByStudentIdAndTermId(Long studentId, Long termId);

    Optional<GeneratedReport> findByIdAndSchoolId(Long id, Long schoolId);

    List<GeneratedReport> findByStudentIdOrderByGeneratedAtDesc(Long studentId);

    List<GeneratedReport> findBySchoolIdAndStudentIdOrderByGeneratedAtDesc(Long schoolId, Long studentId);

    List<GeneratedReport> findBySchoolIdAndStudentIdAndTermIdOrderByGeneratedAtDesc(
            Long schoolId, Long studentId, Long termId);

    List<GeneratedReport> findBySchoolIdAndClassIdAndTermIdOrderByGeneratedAtDesc(
            Long schoolId, Long classId, Long termId);
}
