package com.example.webrtcbackend.config;

import com.example.webrtcbackend.auth.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers("/api/auth/me").authenticated()
                // User management: ADMIN only
                .requestMatchers("/api/users/**").hasRole("ADMIN")
                // Course write operations: LECTURER or ADMIN (ownership enforced in service layer)
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/courses/**").hasAnyRole("LECTURER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/courses/**").hasAnyRole("LECTURER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/courses/**").hasAnyRole("LECTURER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/courses/**").permitAll()
                // Lesson write operations: LECTURER or ADMIN
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/lessons/**").hasAnyRole("LECTURER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/lessons/**").hasAnyRole("LECTURER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/lessons/**").hasAnyRole("LECTURER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/lessons/**").permitAll()
                // Video write operations: LECTURER or ADMIN
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/videos/**").hasAnyRole("LECTURER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/videos/**").permitAll()
                // Other endpoints
                .requestMatchers("/api/transcriptions/**").permitAll()
                .requestMatchers("/api/assignments/**").permitAll()
                .requestMatchers("/api/quiz-results/**").permitAll()
                .requestMatchers("/api/vocabulary/**").permitAll()
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(List.of("*"));

        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "PATCH",
                "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of("*"));

        configuration.setExposedHeaders(List.of(
                "Authorization",
                "Content-Type"
        ));

        configuration.setAllowCredentials(true);

        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
