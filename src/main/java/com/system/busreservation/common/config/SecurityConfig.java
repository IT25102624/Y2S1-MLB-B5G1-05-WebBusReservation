package com.system.busreservation.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration.
 * Stateless permitAll skeleton ready for JWT filter integration.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF since REST APIs are stateless
            .csrf(AbstractHttpConfigurer::disable)
            // Stateless session management
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Authorization rules skeleton: permitAll for initial team development
            // Module 1 (Auth) will plug in JwtAuthenticationFilter before UsernamePasswordAuthenticationFilter
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/search/**").permitAll()
                .requestMatchers("/api/v1/feedback/**", "/api/feedback/**").permitAll()
                .requestMatchers("/api/v1/luggage/**", "/api/luggage/**").permitAll()
                .requestMatchers("/api/v1/notifications/**", "/api/notifications/**").permitAll()
                .requestMatchers("/*.html", "/css/**", "/js/**", "/images/**").permitAll()
                .anyRequest().permitAll()
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
