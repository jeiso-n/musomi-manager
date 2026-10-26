package com.musomi.manager.repository;

import java.util.List;

import com.musomi.manager.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<AuditLog> findByUserIdAndSchoolIdOrderByCreatedAtDesc(Long userId, Long schoolId);

    List<AuditLog> findBySchoolIdOrderByCreatedAtDesc(Long schoolId);

    List<AuditLog> findByActionOrderByCreatedAtDesc(String action);

    List<AuditLog> findBySchoolIdAndActionOrderByCreatedAtDesc(Long schoolId, String action);

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);

    List<AuditLog> findBySchoolIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
            Long schoolId, String entityType, Long entityId);
}
