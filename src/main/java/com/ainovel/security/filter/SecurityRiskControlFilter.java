package com.ainovel.security.filter;

import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.infrastructure.log.AuditAction;
import com.ainovel.infrastructure.log.TraceIdHolder;
import com.ainovel.security.audit.SecurityAuditEvent;
import com.ainovel.security.audit.SecurityAuditService;
import com.ainovel.security.auth.context.CurrentUserHolder;
import com.ainovel.security.config.SecurityProperties;
import com.ainovel.security.risk.RiskControlService;
import com.ainovel.security.risk.RiskDecision;
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
public class SecurityRiskControlFilter extends OncePerRequestFilter {

    private static final String CHAPTER_CONTENT_PATH = "/api/v1/chapters/*/content";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final SecurityProperties securityProperties;
    private final RiskControlService riskControlService;
    private final SecurityFailureResponseWriter responseWriter;
    private final SecurityAuditService securityAuditService;

    public SecurityRiskControlFilter(SecurityProperties securityProperties,
                                     RiskControlService riskControlService,
                                     SecurityFailureResponseWriter responseWriter,
                                     SecurityAuditService securityAuditService) {
        this.securityProperties = securityProperties;
        this.riskControlService = riskControlService;
        this.responseWriter = responseWriter;
        this.securityAuditService = securityAuditService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!securityProperties.riskControlEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }
        RiskPolicy policy = resolvePolicy(request);
        if (policy == null) {
            filterChain.doFilter(request, response);
            return;
        }
        String subject = resolveSubject(policy.scope(), request);
        RiskDecision decision;
        try {
            decision = riskControlService.decide(policy.policyId(), subject, policy.windowSeconds(), policy.threshold());
        } catch (BusinessException ex) {
            securityAuditService.record(new SecurityAuditEvent(
                    AuditAction.AUTH_REDIS_FAILURE,
                    CurrentUserHolder.get().map(currentUser -> currentUser.userId()).orElse(null),
                    request.getHeader("X-Device-Id"),
                    request.getRequestURI(),
                    request.getMethod(),
                    TraceIdHolder.get().orElse(""),
                    "risk_control_redis_unavailable"));
            responseWriter.write(response, StandardErrorCode.DEPENDENCY_UNAVAILABLE);
            return;
        }
        if (decision == RiskDecision.BLOCK) {
            securityAuditService.record(new SecurityAuditEvent(
                    AuditAction.RISK_CONTROL_BLOCKED,
                    CurrentUserHolder.get().map(currentUser -> currentUser.userId()).orElse(null),
                    request.getHeader("X-Device-Id"),
                    request.getRequestURI(),
                    request.getMethod(),
                    TraceIdHolder.get().orElse(""),
                    "policy=" + policy.policyId() + ",subject=" + subject));
            responseWriter.write(response, StandardErrorCode.RISK_BLOCKED);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private RiskPolicy resolvePolicy(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if (HttpMethod.POST.matches(method) && isAuthPath(path)) {
            return new RiskPolicy("auth", "ip", securityProperties.riskAuthWindowSeconds(), securityProperties.riskAuthThreshold());
        }
        if (HttpMethod.POST.matches(method) && "/api/v1/comments".equals(path)) {
            return new RiskPolicy("comment", "user", securityProperties.riskCommentWindowSeconds(), securityProperties.riskCommentThreshold());
        }
        if (HttpMethod.GET.matches(method) && pathMatcher.match(CHAPTER_CONTENT_PATH, path)) {
            return new RiskPolicy("chapter-read", "ip", securityProperties.riskReadWindowSeconds(), securityProperties.riskReadThreshold());
        }
        return null;
    }

    private boolean isAuthPath(String path) {
        return "/api/v1/auth/login".equals(path)
                || "/api/v1/auth/register".equals(path)
                || "/api/v1/auth/refresh".equals(path);
    }

    private String resolveSubject(String scope, HttpServletRequest request) {
        if ("user".equals(scope)) {
            return CurrentUserHolder.get()
                    .map(currentUser -> currentUser.userId() == null ? null : "user:" + currentUser.userId())
                    .orElseGet(() -> "ip:" + resolveClientIp(request));
        }
        return "ip:" + resolveClientIp(request);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr() == null || request.getRemoteAddr().isBlank()
                ? "unknown"
                : request.getRemoteAddr().trim();
    }

    private record RiskPolicy(String policyId, String scope, int windowSeconds, int threshold) {
    }
}
