package com.cupqueue.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configures stateless HTTP security for the backend API.
 *
 * <p>The foundation phase permits application requests so that API development can proceed
 * before authentication is implemented. Authenticated authorization rules must replace the
 * final permit-all rule before deployment.</p>
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    /**
     * Creates the HTTP security configuration.
     */
    public SecurityConfig() {
    }

    /**
     * Builds the servlet security filter chain.
     *
     * @param http the security builder supplied by Spring Security
     * @return the configured security filter chain
     * @throws Exception if Spring Security cannot build the filter chain
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**"
                        ).permitAll()
                        .anyRequest().permitAll())
                .build();
    }
}
