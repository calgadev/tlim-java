package com.tlim.config;

import com.tlim.auth.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Stateless JWT API — no session cookies are issued, so CSRF tokens provide no protection here
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Missing/invalid JWT → 401 (the default for a stateless API without form login would be 403)
            .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
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
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
