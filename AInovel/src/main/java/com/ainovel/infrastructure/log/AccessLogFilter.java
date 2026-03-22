package com.ainovel.infrastructure.log;

import com.ainovel.access.contract.RequestHeaders;
import com.ainovel.security.auth.context.CurrentUserHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AccessLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AccessLogFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            log.info("request summary traceId={} requestId={} method={} path={} status={} elapsedMs={} client={} appVersion={} userId={} ip={} ua={}",
                    TraceIdHolder.get().orElse(""),
                    response.getHeader(TraceIdFilter.REQUEST_ID_HEADER),
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    elapsed,
                    request.getHeader(RequestHeaders.X_CLIENT),
                    request.getHeader(RequestHeaders.X_APP_VERSION),
                    CurrentUserHolder.get().map(currentUser -> currentUser.userId() == null ? "" : currentUser.userId().toString())
                            .orElse(""),
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent"));
        }
    }
}
