package com.ainovel.security.filter;

import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.infrastructure.log.AuditAction;
import com.ainovel.infrastructure.log.TraceIdHolder;
import com.ainovel.security.audit.SecurityAuditEvent;
import com.ainovel.security.audit.SecurityAuditService;
import com.ainovel.security.auth.context.CurrentUserHolder;
import com.ainovel.security.config.SecurityProperties;
import com.ainovel.security.rate.RateLimitDimension;
import com.ainovel.security.rate.limiter.RateLimiterService;
import com.ainovel.security.rate.policy.RateLimitPolicy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SecurityRateLimitFilter extends OncePerRequestFilter {

    private static final String AUTH_POLICY_ID = "auth";
    private static final String COMMENT_POLICY_ID = "comment";
    private static final String CHAPTER_READ_POLICY_ID = "chapter-read";
    private static final String CHAPTER_CONTENT_PATH = "/api/v1/chapters/*/content";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final RateLimiterService rateLimiterService;
    private final SecurityProperties securityProperties;
    private final SecurityFailureResponseWriter responseWriter;
    private final SecurityAuditService securityAuditService;

    public SecurityRateLimitFilter(RateLimiterService rateLimiterService,
                                   SecurityProperties securityProperties,
                                   SecurityFailureResponseWriter responseWriter,
                                   SecurityAuditService securityAuditService) {
        this.rateLimiterService = rateLimiterService;
        this.securityProperties = securityProperties;
        this.responseWriter = responseWriter;
        this.securityAuditService = securityAuditService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!securityProperties.rateLimitEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitPolicy policy = resolvePolicy(request);
        if (policy == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String subject = resolveSubject(policy.dimension(), request);
        boolean allowed;
        try {
            allowed = rateLimiterService.allow(subject, request.getRequestURI(), policy);
        } catch (BusinessException ex) {
            securityAuditService.record(new SecurityAuditEvent(
                    AuditAction.AUTH_REDIS_FAILURE,
                    CurrentUserHolder.get().map(currentUser -> currentUser.userId()).orElse(null),
                    request.getHeader("X-Device-Id"),
                    request.getRequestURI(),
                    request.getMethod(),
                    TraceIdHolder.get().orElse(""),
                    "rate_limit_redis_unavailable"));
            responseWriter.write(response, StandardErrorCode.DEPENDENCY_UNAVAILABLE);
            return;
        }

        if (!allowed) {
            securityAuditService.record(new SecurityAuditEvent(
                    AuditAction.RATE_LIMIT_BLOCKED,
                    CurrentUserHolder.get().map(currentUser -> currentUser.userId()).orElse(null),
                    request.getHeader("X-Device-Id"),
                    request.getRequestURI(),
                    request.getMethod(),
                    TraceIdHolder.get().orElse(""),
                    "policy=" + policy.policyId() + ",subject=" + subject));
            responseWriter.write(response, StandardErrorCode.RATE_LIMITED);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private RateLimitPolicy resolvePolicy(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if (HttpMethod.POST.matches(method) && isAuthPath(path)) {
            return new RateLimitPolicy(
                    AUTH_POLICY_ID,
                    RateLimitDimension.IP,
                    securityProperties.rateLimitAuthWindowSeconds(),
                    securityProperties.rateLimitAuthThreshold());
        }
        if (HttpMethod.POST.matches(method) && "/api/v1/comments".equals(path)) {
            return new RateLimitPolicy(
                    COMMENT_POLICY_ID,
                    RateLimitDimension.USER,
                    securityProperties.rateLimitCommentWindowSeconds(),
                    securityProperties.rateLimitCommentThreshold());
        }
        if (HttpMethod.GET.matches(method) && pathMatcher.match(CHAPTER_CONTENT_PATH, path)) {
            return new RateLimitPolicy(
                    CHAPTER_READ_POLICY_ID,
                    RateLimitDimension.IP,
                    securityProperties.rateLimitReadWindowSeconds(),
                    securityProperties.rateLimitReadThreshold());
        }
        return null;
    }

    private boolean isAuthPath(String path) {
        return "/api/v1/auth/login".equals(path)
                || "/api/v1/auth/register".equals(path)
                || "/api/v1/auth/refresh".equals(path);
    }

    private String resolveSubject(RateLimitDimension dimension, HttpServletRequest request) {
        return switch (dimension) {
            case USER -> CurrentUserHolder.get()
                    .map(currentUser -> currentUser.userId() == null ? null : "user:" + currentUser.userId())
                    .orElseGet(() -> "ip:" + resolveClientIp(request));
            case DEVICE -> "device:" + normalize(request.getHeader("X-Device-Id"), "unknown");
            case PATH -> "path:" + request.getRequestURI();
            case IP -> "ip:" + resolveClientIp(request);
        };
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return normalize(request.getRemoteAddr(), "unknown");
    }

    private String normalize(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }
}
