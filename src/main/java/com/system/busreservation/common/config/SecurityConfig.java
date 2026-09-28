package com.system.busreservation.common.config;

import com.system.busreservation.modules.auth.security.CustomUserDetailsService;
import com.system.busreservation.modules.auth.security.JwtAuthenticationFilter;
import com.system.busreservation.modules.auth.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration.
 * Stateless JWT authentication filter integrated with RBAC support.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(
            @Autowired(required = false) @Lazy JwtAuthenticationFilter jwtAuthenticationFilter,
            @Autowired(required = false) @Lazy CustomUserDetailsService userDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (userDetailsService != null) {
            http.authenticationProvider(daoAuthenticationProvider());
        }

        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/auth/**", "/api/v1/auth/**").permitAll()
            .requestMatchers("/search/**", "/api/v1/search/**").permitAll()
            .requestMatchers("/", "/index.html", "/search.html", "/auth.html", "/admin.html", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
            .requestMatchers("/admin/**", "/api/v1/admin/**").permitAll()
            .anyRequest().permitAll()
        );

        if (jwtAuthenticationFilter != null) {
            http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        if (userDetailsService != null) {
            provider.setUserDetailsService(userDetailsService);
        }
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            @Autowired(required = false) JwtTokenProvider jwtTokenProvider,
            @Autowired(required = false) CustomUserDetailsService customUserDetailsService) {
        if (jwtTokenProvider != null && customUserDetailsService != null) {
            return new JwtAuthenticationFilter(jwtTokenProvider, customUserDetailsService);
        }
        return null;
    }
}
