package com.musomi.manager.repository;

import com.musomi.manager.entity.User;
import com.musomi.manager.entity.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findBySchoolIdAndUsername(Long schoolId, String username);

    Optional<User> findByUsername(String username);

    Optional<User> findByIdAndSchoolId(Long id, Long schoolId);

    List<User> findBySchoolIdAndRole(Long schoolId, Role role);

    List<User> findBySchoolIdAndIsActiveTrue(Long schoolId);

    boolean existsBySchoolIdAndUsername(Long schoolId, String username);

}