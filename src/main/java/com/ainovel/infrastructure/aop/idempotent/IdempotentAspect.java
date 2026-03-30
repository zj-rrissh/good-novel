package com.ainovel.infrastructure.aop.idempotent;

import com.ainovel.access.contract.IdempotencyScope;
import com.ainovel.access.service.IdempotencyService;
import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.infrastructure.aop.spel.SpelExpressionEvaluator;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.security.auth.context.CurrentUserHolder;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Order(1)
public class IdempotentAspect {

    private final IdempotencyService idempotencyService;
    private final SpelExpressionEvaluator spelEvaluator;

    public IdempotentAspect(IdempotencyService idempotencyService,
                            SpelExpressionEvaluator spelEvaluator) {
        this.idempotencyService = idempotencyService;
        this.spelEvaluator = spelEvaluator;
    }

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        String idempotencyKey = resolveKey(idempotent, method, args, signature.getParameterNames());
        if (idempotencyKey == null) {
            return joinPoint.proceed();
        }

        Long userId = CurrentUserHolder.get().map(u -> u.userId()).orElse(null);
        String requestPath = resolveRequestPath();

        IdempotencyScope scope = new IdempotencyScope(userId, requestPath, idempotencyKey);
        Duration ttl = Duration.of(idempotent.ttl(), idempotent.timeUnit().toChronoUnit());

        boolean firstTime = idempotencyService.record(scope, ttl);
        if (!firstTime) {
            String message = idempotent.message().isEmpty()
                    ? StandardErrorCode.IDEMPOTENT_CONFLICT.getMessage()
                    : idempotent.message();
            throw new BusinessException(StandardErrorCode.IDEMPOTENT_CONFLICT, message);
        }

        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            idempotencyService.release(scope);
            throw ex;
        }
    }

    private String resolveKey(Idempotent idempotent, Method method, Object[] args, String[] paramNames) {
        return switch (idempotent.strategy()) {
            case TOKEN -> resolveTokenKey(idempotent, args, paramNames);
            case FINGERPRINT -> resolveFingerprintKey(idempotent, method, args);
        };
    }

    private String resolveTokenKey(Idempotent idempotent, Object[] args, String[] paramNames) {
        if (paramNames == null || paramNames.length == 0) {
            if (idempotent.optional()) {
                return null;
            }
            throw new BusinessException(StandardErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }
        String tokenValue = null;
        for (int i = 0; i < paramNames.length; i++) {
            if (paramNames[i].equals(idempotent.tokenParam())) {
                tokenValue = args[i] == null ? null : args[i].toString();
                break;
            }
        }

        if (tokenValue == null || tokenValue.isBlank()) {
            if (idempotent.optional()) {
                return null;
            }
            throw new BusinessException(StandardErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }
        return tokenValue;
    }

    private String resolveFingerprintKey(Idempotent idempotent, Method method, Object[] args) {
        if (idempotent.fingerprint() == null || idempotent.fingerprint().isBlank()) {
            throw new BusinessException(StandardErrorCode.INVALID_REQUEST, "fingerprint expression is required");
        }
        String raw = spelEvaluator.evaluate(idempotent.fingerprint(), method, args);
        return sha256(raw);
    }

    private String resolveRequestPath() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            return request.getRequestURI();
        }
        return "unknown";
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
