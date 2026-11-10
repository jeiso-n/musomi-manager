package com.musomi.manager.security;

import com.musomi.manager.entity.User;
import com.musomi.manager.repository.UserRepository;
import com.musomi.manager.repository.StudentRepository;
import com.musomi.manager.entity.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        int schoolSeparator = username == null ? -1 : username.indexOf("::");
        if (schoolSeparator > 0) {
            try {
                Long selectedSchoolId = Long.valueOf(username.substring(0, schoolSeparator));
                String selectedUsername = username.substring(schoolSeparator + 2);
                if (selectedUsername.isBlank()) {
                    throw new NumberFormatException("Missing username");
                }
                return loadUserByUsername(selectedUsername, selectedSchoolId);
            } catch (NumberFormatException exception) {
                throw new UsernameNotFoundException("Invalid school or username", exception);
            }
        }
        return loadUserByUsername(username, null);
    }

    /**
     * Resolves a login within the selected school.
     */
    public UserDetails loadUserByUsername(String username, Long schoolId)
            throws UsernameNotFoundException {
        User user = (schoolId == null
                ? userRepository.findByUsername(username)
                : userRepository.findBySchoolIdAndUsername(schoolId, username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new UsernameNotFoundException("Account is inactive");
        }

        if (user.getRole() == Role.STUDENT
                && studentRepository.findFirstBySchoolIdAndUserId(user.getSchool().getId(), user.getId())
                        .filter(student -> Boolean.TRUE.equals(student.getIsActive()))
                        .filter(student -> "ACTIVE".equalsIgnoreCase(student.getStatus())).isEmpty()) {
            throw new UsernameNotFoundException("No active student record is linked to this account");
        }

        return new WebUserPrincipal(user.getUsername(), user.getPasswordHash(),
                true, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "ROLE_" + user.getRole().name())), user.getSchool().getId());
    }
}
