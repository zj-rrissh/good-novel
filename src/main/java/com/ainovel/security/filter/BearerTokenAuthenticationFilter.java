package com.ainovel.security.filter;

import com.ainovel.access.contract.RequestHeaders;
import com.ainovel.security.auth.context.CurrentUser;
import com.ainovel.security.auth.context.CurrentUserHolder;
import com.ainovel.security.auth.token.AccessTokenClaims;
import com.ainovel.security.auth.token.JwtTokenProvider;
import com.ainovel.user.service.support.AuthSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthSessionService authSessionService;

    public BearerTokenAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                           AuthSessionService authSessionService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.authSessionService = authSessionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader(RequestHeaders.AUTHORIZATION);
        String deviceId = request.getHeader(RequestHeaders.X_DEVICE_ID);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring("Bearer ".length()).trim();
            jwtTokenProvider.parse(token)
                    .filter(authSessionService::isAccessTokenActive)
                    .ifPresent(claims -> authenticate(deviceId, claims));
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
            CurrentUserHolder.clear();
        }
    }

    private void authenticate(String deviceId, AccessTokenClaims claims) {
        Long userId = claims.subject() == null ? null : Long.parseLong(claims.subject());
        CurrentUserHolder.set(new CurrentUser(
                userId,
                claims.roles(),
                deviceId,
                true,
                claims.jti(),
                claims.tokenVersion(),
                claims.expiresAt()));
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                claims.subject(),
                null,
                claims.roles().stream()
                        .map(roleType -> new SimpleGrantedAuthority("ROLE_" + roleType.name()))
                        .collect(Collectors.toSet()));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }
}
