package com.ainovel.access.web;

import com.ainovel.access.contract.ApiPaths;
import com.ainovel.access.contract.ClientType;
import com.ainovel.access.contract.RequestHeaders;
import com.ainovel.access.support.ClientTypeResolver;
import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.infrastructure.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AccessHeaderInterceptor implements HandlerInterceptor {

    private final ClientTypeResolver clientTypeResolver;

    public AccessHeaderInterceptor(ClientTypeResolver clientTypeResolver) {
        this.clientTypeResolver = clientTypeResolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            return true;
        }
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String client = request.getHeader(RequestHeaders.X_CLIENT);
        if (client == null || client.isBlank()) {
            throw new BusinessException(StandardErrorCode.INVALID_REQUEST, "missing X-Client header");
        }
        ClientType clientType = clientTypeResolver.resolve(client)
                .orElseThrow(() -> new BusinessException(StandardErrorCode.INVALID_CLIENT_HEADER, "unsupported X-Client header"));
        if ((path.startsWith(ApiPaths.API_ADMIN_V1) || path.startsWith(ApiPaths.API_V1_ADMIN)) && clientType != ClientType.ADMIN) {
            throw new BusinessException(StandardErrorCode.INVALID_CLIENT_HEADER, "admin path requires X-Client=admin");
        }
        if (path.startsWith(ApiPaths.API_V1 + "/internal") && clientType != ClientType.INTERNAL) {
            throw new BusinessException(StandardErrorCode.INVALID_CLIENT_HEADER, "internal path requires X-Client=internal");
        }
        if (path.startsWith(ApiPaths.API_V1)
                && !path.startsWith(ApiPaths.API_V1 + "/internal")
                && clientType != ClientType.PC_WEB
                && clientType != ClientType.H5) {
            throw new BusinessException(StandardErrorCode.INVALID_CLIENT_HEADER, "user path requires X-Client=pc-web|h5");
        }
        return true;
    }
}
