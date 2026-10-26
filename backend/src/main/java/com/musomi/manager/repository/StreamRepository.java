package com.musomi.manager.repository;

import java.util.List;

import com.musomi.manager.entity.Stream;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StreamRepository extends JpaRepository<Stream, Long> {

    List<Stream> findByClassEntityIdAndIsActiveTrue(Long classId);

    List<Stream> findByClassEntityIdAndSchoolIdAndIsActiveTrue(Long classId, Long schoolId);

    boolean existsByClassEntityIdAndName(Long classId, String name);
}
