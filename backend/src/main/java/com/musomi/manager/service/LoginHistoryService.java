package com.musomi.manager.service;

import java.time.LocalDateTime;
import java.util.List;

import com.musomi.manager.dto.response.LoginHistoryResponse;
import com.musomi.manager.entity.LoginHistory;
import com.musomi.manager.entity.School;
import com.musomi.manager.entity.User;
import com.musomi.manager.repository.LoginHistoryRepository;
import com.musomi.manager.util.SchoolScope;
import com.musomi.manager.mapper.LoginHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages login history records. */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginHistoryService {

    private final LoginHistoryRepository loginHistoryRepository;

    /** Lists login attempts for a user. */
    @Transactional(readOnly = true)
    public List<LoginHistoryResponse> listByUser(Long userId, Long schoolId) {
        return LoginHistoryMapper.toResponseList(
                loginHistoryRepository.findByUserIdAndSchoolIdOrderByCreatedAtDesc(userId, SchoolScope.require(schoolId)));
    }

    /** Lists login attempts for a school. */
    @Transactional(readOnly = true)
    public List<LoginHistoryResponse> listBySchool(Long schoolId) {
        return LoginHistoryMapper.toResponseList(
                loginHistoryRepository.findBySchoolIdOrderByCreatedAtDesc(SchoolScope.require(schoolId)));
    }

    /** Records a login attempt, retaining unknown usernames without a user association. */
    @Transactional
    public void record(String usernameAttempted, Long userId, Long schoolId,
                       boolean success, String ip, String userAgent) {
        User user = userId != null ? User.builder().id(userId).build() : null;
        LoginHistory history = LoginHistory.builder()
                .school(School.builder().id(SchoolScope.require(schoolId)).build())
                .user(user)
                .usernameAttempted(usernameAttempted)
                .success(success)
                .ipAddress(ip)
                .userAgent(userAgent)
                .createdAt(LocalDateTime.now())
                .build();
        loginHistoryRepository.save(history);
        log.info("Login attempt recorded: username={}, success={}", usernameAttempted, success);
    }
}
