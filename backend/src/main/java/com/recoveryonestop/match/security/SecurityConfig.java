package com.recoveryonestop.match.security;

import com.recoveryonestop.match.auth.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
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
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final List<String> allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RestAuthenticationEntryPoint authenticationEntryPoint,
                          RestAccessDeniedHandler accessDeniedHandler,
                          @Value("${security.cors.allowed-origins:http://localhost:5173}")
                          String allowedOrigins) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(authorize -> authorize
                        // ⚠️ 2026-10-02: /api/match는 사실 처음부터 POST 전용
                        // (@RequestBody MatchRequest) 엔드포인트였다. 중간에 GET으로
                        // permitAll을 걸었다가 "405 Request method 'GET' is not
                        // supported"를 401처럼 보이는 공통 에러 포맷으로 가려서 헷갈렸던
                        //것뿐 — 실제 호출은 항상 POST + JSON body여야 한다.
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/signup",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/logout",
                                "/api/match")
                        .permitAll()
                        // /api/disasters는 실제로 @GetMapping이라 GET이 맞다.
                        .requestMatchers(HttpMethod.GET, "/api/disasters")
                        .permitAll()
                        // ⚠️ 2026-10-07: AdminSyncController는 이미 @Profile("local")로
                        // 로컬 개발 프로파일에서만 존재하는 임시 테스트용 컨트롤러다
                        // (운영 배포 시 profile 자체가 local이 아니면 빈이 안 만들어짐).
                        // sync-central/sync-local 두 개만 콕 집어 permitAll 하던 걸
                        // 엔드포인트 추가할 때마다 깜빡하고 안 넣어서(assign-required-docs가
                        // 바로 그 사례) 401이 났던 거라, 아예 /api/admin/** 전체를 허용한다.
                        .requestMatchers("/api/admin/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
