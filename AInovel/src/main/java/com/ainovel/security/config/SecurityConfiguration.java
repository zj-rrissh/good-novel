package com.ainovel.security.config;

import com.ainovel.security.filter.BearerTokenAuthenticationFilter;
import com.ainovel.security.filter.SecurityRateLimitFilter;
import com.ainovel.security.filter.SecurityRiskControlFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter,
                                                   SecurityRateLimitFilter securityRateLimitFilter,
                                                   SecurityRiskControlFilter securityRiskControlFilter,
                                                   SecurityProperties securityProperties) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> {
                    securityProperties.whitelistPaths().forEach(path -> authorize.requestMatchers(path).permitAll());
                    authorize.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll();
                    authorize.requestMatchers(HttpMethod.GET,
                            "/api/v1/novels/*",
                            "/api/v1/novels/*/chapters",
                            "/api/v1/chapters/*/content",
                            "/api/v1/counters",
                            "/api/v1/recommend/**",
                            "/api/v1/gateway/contract").permitAll();
                    authorize.requestMatchers("/api/admin/**", "/api/v1/admin/**").hasAnyRole("ADMIN", "REVIEWER");
                    authorize.requestMatchers("/api/v1/internal/**").hasRole("INTERNAL");
                    authorize.requestMatchers(
                            "/api/v1/users/me/**",
                            "/api/v1/auth/logout",
                            "/api/v1/comments/**",
                            "/api/v1/reactions/**",
                            "/api/v1/reading-progress").authenticated();
                    authorize.requestMatchers(HttpMethod.POST,
                            "/api/v1/users/*/follow",
                            "/api/v1/users/*/unfollow",
                            "/api/v1/novels",
                            "/api/v1/novels/*/chapters",
                            "/api/v1/novels/*/submit-audit",
                            "/api/v1/novels/*/on-shelf",
                            "/api/v1/novels/*/off-shelf").authenticated();
                    authorize.requestMatchers(HttpMethod.PUT, "/api/v1/novels/*", "/api/v1/chapters/*").authenticated();
                    authorize.anyRequest().denyAll();
                })
                .addFilterBefore(bearerTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(securityRateLimitFilter, BearerTokenAuthenticationFilter.class)
                .addFilterAfter(securityRiskControlFilter, SecurityRateLimitFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
