package com.musomi.manager.repository;

import java.util.List;

import com.musomi.manager.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    List<Topic> findBySubjectIdOrderBySortOrderAsc(Long subjectId);

    List<Topic> findBySubjectIdAndSchoolIdOrderBySortOrderAsc(Long subjectId, Long schoolId);

    boolean existsBySubjectIdAndName(Long subjectId, String name);
}
