package com.tlim.config;

import com.tlim.auth.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    private final List<String> allowedOrigins;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          @Value("${app.cors.allowed-origins:http://localhost:5173}") List<String> allowedOrigins) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Stateless JWT API — no session cookies are issued, so CSRF tokens provide no protection here
            .csrf(csrf -> csrf.disable())
            // Browser frontends on another origin (e.g. the Vite dev server); preflights are answered before auth
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Missing/invalid JWT → 401 (the default for a stateless API without form login would be 403).
            // Both security errors use the same JSON body as GlobalExceptionHandler.
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(jsonAuthenticationEntryPoint())
                .accessDeniedHandler(jsonAccessDeniedHandler()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/api-docs/**").permitAll()
                // Error dispatches skip the JWT filter; without this a 403/404/409 is rewritten as 401
                .requestMatchers("/error").permitAll()
                // ADMIN role guard applied at the URL level — not via per-method annotations that could be omitted
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // Market prices are per-server user data, editable by any authenticated user.
                // Must be declared before the /api/servers/** catalog rule below.
                .requestMatchers("/api/servers/*/item-prices", "/api/servers/*/item-prices/**").authenticated()
                // Servers, items and creatures are a shared catalog: anyone can read, only ADMIN can change
                .requestMatchers(HttpMethod.POST, "/api/servers/**", "/api/items/**", "/api/creatures/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/servers/**", "/api/items/**", "/api/creatures/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/servers/**", "/api/items/**", "/api/creatures/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/servers/**", "/api/items/**", "/api/creatures/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins.stream().map(String::trim).filter(o -> !o.isEmpty()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    private static AuthenticationEntryPoint jsonAuthenticationEntryPoint() {
        return (request, response, ex) ->
                writeError(response, HttpStatus.UNAUTHORIZED, "Missing or invalid authentication token");
    }

    private static AccessDeniedHandler jsonAccessDeniedHandler() {
        return (request, response, ex) -> writeError(response, HttpStatus.FORBIDDEN, "Access denied");
    }

    private static void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // Fixed messages only; nothing user-supplied is echoed, so no JSON escaping is needed
        response.getWriter().write("{\"status\":" + status.value()
                + ",\"error\":\"" + status.getReasonPhrase()
                + "\",\"message\":\"" + message + "\"}");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
