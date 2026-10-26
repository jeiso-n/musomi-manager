package com.musomi.manager.config;

import com.musomi.manager.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login", "/css/**", "/js/**", "/img/**", "/error", "/error/**",
                                "/favicon.ico", "/", "/api/v1/auth/login", "/actuator/health")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/admin/classes",
                                "/api/v1/admin/subjects",
                                "/api/v1/admin/subjects/*/topics",
                                "/api/v1/admin/terms")
                        .hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers("/api/v1/**").authenticated()
                        .requestMatchers("/student/**").hasRole("STUDENT")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"error\":{\"code\":\"SESSION_EXPIRED\","
                                            + "\"message\":\"Your session has expired. Please log in again.\","
                                            + "\"details\":{}}}"
                            );
                        }, request -> request.getServletPath().startsWith("/api/"))
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/student/dashboard")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login")
                        .permitAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
