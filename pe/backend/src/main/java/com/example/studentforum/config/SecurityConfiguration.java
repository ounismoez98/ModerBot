package com.example.studentforum.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfiguration {

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    UserDetailsService moderatorUser(
            @Value("${app.moderator.username}") String username,
            @Value("${app.moderator.password}") String password,
            @Value("${app.student.username}") String studentUsername,
            @Value("${app.student.password}") String studentPassword,
            PasswordEncoder passwordEncoder
    ) {
        if (username.isBlank() || password.isBlank() || studentUsername.isBlank() || studentPassword.isBlank()) {
            throw new IllegalStateException(
                    "MODERATOR_USERNAME, MODERATOR_PASSWORD, STUDENT_USERNAME et STUDENT_PASSWORD doivent être configurés."
            );
        }
        if (username.equalsIgnoreCase(studentUsername)) {
            throw new IllegalStateException("Le compte étudiant et le compte modérateur doivent avoir des identifiants différents.");
        }
        return new InMemoryUserDetailsManager(
                User.withUsername(username)
                        .password(passwordEncoder.encode(password))
                        .roles("MODERATOR")
                        .build(),
                User.withUsername(studentUsername)
                        .password(passwordEncoder.encode(studentPassword))
                        .roles("STUDENT")
                        .build()
        );
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository
    ) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .securityContext(context -> context
                        .securityContextRepository(securityContextRepository)
                        .requireExplicitSave(true)
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/moderation/queue/**").hasRole("MODERATOR")
                        .requestMatchers("/api/moderation/**").hasRole("MODERATOR")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/posts/**").hasRole("MODERATOR")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/posts/**").hasRole("MODERATOR")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/posts").hasRole("STUDENT")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/posts/**")
                        .hasAnyRole("STUDENT", "MODERATOR")
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/me", "/api/auth/logout").hasAnyRole("STUDENT", "MODERATOR")
                        .anyRequest().denyAll()
                )
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
