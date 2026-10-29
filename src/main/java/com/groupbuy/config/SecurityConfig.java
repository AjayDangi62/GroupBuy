package com.groupbuy.config;

import com.groupbuy.auth.security.JwtAuthenticationFilter;
import com.groupbuy.auth.security.SecurityAccessDeniedHandler;
import com.groupbuy.auth.security.SecurityAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration for GroupBuy.
 * Enforces stateless REST semantics, environment-based CORS, BCrypt hashing (strength 12),
 * and JWT bearer token authentication filter.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({CorsProperties.class, JwtProperties.class})
public class SecurityConfig {

    private final CorsProperties corsProperties;

    public SecurityConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Autowired(required = false) JwtAuthenticationFilter jwtAuthenticationFilter,
            @Autowired(required = false) SecurityAuthenticationEntryPoint authenticationEntryPoint,
            @Autowired(required = false) SecurityAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        http
                // Disable CSRF for stateless RESTful APIs
                .csrf(AbstractHttpConfigurer::disable)
                // Configure explicit CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Stateless session management
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Request authorization
                .authorizeHttpRequests(auth -> auth
                        // Public authentication endpoints
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // Public pricing quote calculation endpoint
                        .requestMatchers("/api/v1/pricing/**").permitAll()
                        // Public catalog browsing (Categories, Products, Campaigns)
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/categories", "/api/v1/categories/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/products", "/api/v1/products/{id:[0-9]+}").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/campaigns", "/api/v1/campaigns/{id:[0-9]+}").permitAll()
                        // Public payment gateway asynchronous webhooks
                        .requestMatchers("/api/v1/payments/webhook").permitAll()
                        // Public Actuator health endpoint
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        // OpenAPI 3.0 / Swagger UI documentation assets
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        // Protected endpoints require authentication
                        .anyRequest().authenticated()
                );

        if (authenticationEntryPoint != null || accessDeniedHandler != null) {
            http.exceptionHandling(exceptions -> {
                if (authenticationEntryPoint != null) {
                    exceptions.authenticationEntryPoint(authenticationEntryPoint);
                }
                if (accessDeniedHandler != null) {
                    exceptions.accessDeniedHandler(accessDeniedHandler);
                }
            });
        }

        if (jwtAuthenticationFilter != null) {
            http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }

    /**
     * BCrypt password encoder with cost factor 12 for strong cryptographic hashing.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(com.groupbuy.auth.security.JwtTokenProvider.class)
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            com.groupbuy.auth.security.JwtTokenProvider jwtTokenProvider,
            com.groupbuy.auth.security.CustomUserDetailsService userDetailsService
    ) {
        return new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(corsProperties.getAllowedMethods());
        configuration.setAllowedHeaders(List.of(corsProperties.getAllowedHeaders().split(",")));
        configuration.setAllowCredentials(corsProperties.isAllowCredentials());
        configuration.setMaxAge(corsProperties.getMaxAgeSeconds());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
