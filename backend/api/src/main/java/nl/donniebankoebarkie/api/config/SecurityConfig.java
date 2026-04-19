package nl.donniebankoebarkie.api.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nl.donniebankoebarkie.api.exception.ApiErrorResponse;
import nl.donniebankoebarkie.api.exception.ApiErrorResponseFactory;
import nl.donniebankoebarkie.api.security.JwtAuthenticationFilter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

import java.security.Security;

@Configuration
public class SecurityConfig {
    private static final int ARGON2_SALT_LENGTH = 16;
    private static final int ARGON2_HASH_LENGTH = 32;
    private static final int ARGON2_PARALLELISM = 1;
    private static final int ARGON2_MEMORY_IN_KIB = 19456;
    private static final int ARGON2_ITERATIONS = 2;

    private final ApiErrorResponseFactory apiErrorResponseFactory;
    private final ObjectMapper objectMapper;

    public SecurityConfig(ApiErrorResponseFactory apiErrorResponseFactory, ObjectMapper objectMapper) {
        this.apiErrorResponseFactory = apiErrorResponseFactory;
        this.objectMapper = objectMapper;
    }

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(
                ARGON2_SALT_LENGTH,
                ARGON2_HASH_LENGTH,
                ARGON2_PARALLELISM,
                ARGON2_MEMORY_IN_KIB,
                ARGON2_ITERATIONS);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter)
            throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/logout")
                        .permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(this::writeUnauthorizedError)
                        .accessDeniedHandler(this::writeForbiddenError))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private void writeUnauthorizedError(
            HttpServletRequest request,
            HttpServletResponse response,
            Exception exception) throws java.io.IOException {
        writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "Unauthorized", exception.getMessage(),
                request.getRequestURI());
    }

    private void writeForbiddenError(
            HttpServletRequest request,
            HttpServletResponse response,
            Exception exception) throws java.io.IOException {
        writeErrorResponse(response, HttpStatus.FORBIDDEN, "Forbidden", exception.getMessage(),
                request.getRequestURI());
    }

    private void writeErrorResponse(
            HttpServletResponse response,
            HttpStatus status,
            String error,
            String message,
            String path) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String safeMessage = message == null || message.isBlank() ? error : message;
        ApiErrorResponse errorResponse = apiErrorResponseFactory.create(status, safeMessage, path);
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
