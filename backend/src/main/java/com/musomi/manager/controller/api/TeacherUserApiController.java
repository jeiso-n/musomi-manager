package com.musomi.manager.controller.api;

import java.util.List;

import com.musomi.manager.dto.response.ApiResponse;
import com.musomi.manager.dto.response.UserResponse;
import com.musomi.manager.entity.enums.Role;
import com.musomi.manager.mapper.UserMapper;
import com.musomi.manager.repository.UserRepository;
import com.musomi.manager.util.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teacher/teachers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
public class TeacherUserApiController {

    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<List<UserResponse>> listTeachers() {
        return ApiResponse.success(UserMapper.toResponseList(
                userRepository.findBySchoolIdAndRole(CurrentUser.getSchoolId(), Role.TEACHER)
                        .stream()
                        .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
                        .toList()));
    }
}
