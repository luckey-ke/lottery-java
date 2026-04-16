package com.lottery.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring Security 配置 — 无状态 JWT 认证 + RBAC 角色权限
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // ===== 公开接口 =====
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/auth/register").permitAll()
                .requestMatchers("/api/auth/refresh").permitAll()
                .requestMatchers("/api/auth/config").permitAll()

                // ===== 系统管理接口 — 仅管理员 =====
                .requestMatchers("/api/system/users/**").hasRole("ADMIN")
                .requestMatchers("/api/system/roles/**").hasRole("ADMIN")
                .requestMatchers("/api/system/menus/**").hasRole("ADMIN")

                // ===== GET 读取接口 — 公开（彩票数据） =====
                .requestMatchers(HttpMethod.GET, "/api/lottery/status").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/lottery/results").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/lottery/latest").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/lottery/analyze/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/lottery/trend").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/lottery/recommend").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/lottery/recommend/history").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/lottery/recommend/stats").permitAll()

                // ===== 数据拉取 — 需要 ADMIN 角色或 lottery:lottery:fetch 权限 =====
                .requestMatchers(HttpMethod.POST, "/api/lottery/fetch/**").hasAnyRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/lottery/fetch/**").hasAnyRole("ADMIN")

                // ===== 其他需要认证 =====
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("code", 401);
                    body.put("error", "未登录，请先登录");
                    response.getWriter().write(objectMapper.writeValueAsString(body));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("code", 403);
                    body.put("error", "权限不足");
                    response.getWriter().write(objectMapper.writeValueAsString(body));
                })
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
