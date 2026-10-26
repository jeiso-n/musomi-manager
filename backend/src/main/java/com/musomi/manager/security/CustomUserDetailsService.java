package com.musomi.manager.security;

import com.musomi.manager.entity.User;
import com.musomi.manager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
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

        return new WebUserPrincipal(user.getUsername(), user.getPasswordHash(),
                true, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "ROLE_" + user.getRole().name())), user.getSchool().getId());
    }
}
