package com.musomi.manager.repository;

import java.util.List;

import com.musomi.manager.entity.ClassSubject;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassSubjectRepository extends JpaRepository<ClassSubject, Long> {

    List<ClassSubject> findByClassLevelId(Long classLevelId);

    List<ClassSubject> findBySubjectId(Long subjectId);

    boolean existsByClassLevelIdAndSubjectId(Long classLevelId, Long subjectId);

    void deleteBySubjectId(Long subjectId);
}
