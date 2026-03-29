package com.ainovel.user.service;

import com.ainovel.common.api.PageResponse;
import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.infrastructure.log.AuditAction;
import com.ainovel.infrastructure.log.TraceIdHolder;
import com.ainovel.persistence.support.DelimitedValueCodec;
import com.ainovel.security.audit.SecurityAuditEvent;
import com.ainovel.security.audit.SecurityAuditService;
import com.ainovel.security.auth.context.CurrentUserHolder;
import com.ainovel.user.domain.UserAccount;
import com.ainovel.user.domain.UserRole;
import com.ainovel.user.domain.UserStatus;
import com.ainovel.user.dto.ChangePasswordRequest;
import com.ainovel.user.dto.LoginRequest;
import com.ainovel.user.dto.RefreshTokenRequest;
import com.ainovel.user.dto.RegisterRequest;
import com.ainovel.user.entity.UserAccountEntity;
import com.ainovel.user.entity.UserLoginRecordEntity;
import com.ainovel.user.entity.UserProfileEntity;
import com.ainovel.user.mapper.UserAccountMapper;
import com.ainovel.user.mapper.UserLoginRecordMapper;
import com.ainovel.user.mapper.UserProfileMapper;
import com.ainovel.user.service.support.AuthRefreshSession;
import com.ainovel.user.service.support.AuthSessionService;
import com.ainovel.user.vo.AccessTokenVO;
import com.ainovel.user.vo.LoginRecordVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 30;

    private final UserAccountMapper userAccountMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserLoginRecordMapper userLoginRecordMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthSessionService authSessionService;
    private final SecurityAuditService securityAuditService;

    public AuthServiceImpl(UserAccountMapper userAccountMapper,
                           UserProfileMapper userProfileMapper,
                           UserLoginRecordMapper userLoginRecordMapper,
                           PasswordEncoder passwordEncoder,
                           AuthSessionService authSessionService,
                           SecurityAuditService securityAuditService) {
        this.userAccountMapper = userAccountMapper;
        this.userProfileMapper = userProfileMapper;
        this.userLoginRecordMapper = userLoginRecordMapper;
        this.passwordEncoder = passwordEncoder;
        this.authSessionService = authSessionService;
        this.securityAuditService = securityAuditService;
    }

    @Override
    @Transactional
    public AccessTokenVO register(RegisterRequest request, String idempotencyKey) {
        if (userAccountMapper.findByUsername(request.username()) != null) {
            throw new BusinessException(StandardErrorCode.INVALID_REQUEST, "username already exists");
        }

        UserAccountEntity accountEntity = new UserAccountEntity();
        accountEntity.setUsername(request.username().trim());
        accountEntity.setPasswordHash(passwordEncoder.encode(request.password()));
        accountEntity.setStatus(UserStatus.NORMAL);
        accountEntity.setRoles(DelimitedValueCodec.formatUserRoles(Set.of(UserRole.USER, UserRole.AUTHOR)));
        accountEntity.setLoginVersion(1L);
        try {
            userAccountMapper.insert(accountEntity);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(StandardErrorCode.INVALID_REQUEST, "username already exists");
        }

        UserProfileEntity profileEntity = new UserProfileEntity();
        profileEntity.setUserId(accountEntity.getId());
        String nickname = normalizeOptionalText(request.nickname());
        profileEntity.setNickname(nickname == null ? request.username().trim() : nickname);
        profileEntity.setAvatarUrl(normalizeOptionalText(request.avatarUrl()));
        profileEntity.setBio("");
        profileEntity.setLevel(1);
        profileEntity.setVerifiedStatus("UNVERIFIED");
        userProfileMapper.insert(profileEntity);

        AccessTokenVO accessTokenVO = authSessionService.issueTokens(toDomain(accountEntity), request.deviceId());
        audit(AuditAction.LOGIN, accountEntity.getId(), request.deviceId(), "/api/v1/auth/register", "POST", "register_success");
        return accessTokenVO;
    }

    @Override
    @Transactional(noRollbackFor = com.ainovel.infrastructure.exception.BusinessException.class)
    public AccessTokenVO login(LoginRequest request) {
        String username = request.username().trim();
        UserAccountEntity accountEntity = userAccountMapper.findByUsername(username);

        if (accountEntity == null || !passwordMatches(request.password(), accountEntity.getPasswordHash())) {
            if (accountEntity != null) {
                handleFailedLogin(accountEntity, request.deviceId());
            } else {
                recordLoginAttempt(null, username, false, request.deviceId(), false);
            }
            audit(AuditAction.LOGIN, accountEntity != null ? accountEntity.getId() : null,
                    request.deviceId(), "/api/v1/auth/login", "POST", "login_failed");
            throw new BusinessException(StandardErrorCode.UNAUTHENTICATED, "invalid username or password");
        }

        if (accountEntity.getStatus() != UserStatus.NORMAL) {
            recordLoginAttempt(accountEntity.getId(), username, false, request.deviceId(), false);
            audit(AuditAction.LOGIN, accountEntity.getId(), request.deviceId(), "/api/v1/auth/login", "POST", "account_disabled");
            throw new BusinessException(StandardErrorCode.ACCOUNT_DISABLED);
        }

        LocalDateTime lockedUntil = accountEntity.getLockedUntil();
        if (lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now())) {
            recordLoginAttempt(accountEntity.getId(), username, false, request.deviceId(), false);
            audit(AuditAction.LOGIN, accountEntity.getId(), request.deviceId(), "/api/v1/auth/login", "POST", "account_locked");
            throw new BusinessException(StandardErrorCode.ACCOUNT_DISABLED, "account temporarily locked due to too many failed attempts");
        }

        userAccountMapper.resetFailedLoginCount(accountEntity.getId());
        recordLoginAttempt(accountEntity.getId(), username, true, request.deviceId(), false);
        AccessTokenVO accessTokenVO = authSessionService.issueTokens(toDomain(accountEntity), request.deviceId());
        audit(AuditAction.LOGIN, accountEntity.getId(), request.deviceId(), "/api/v1/auth/login", "POST", "login_success");
        return accessTokenVO;
    }

    private void handleFailedLogin(UserAccountEntity accountEntity, String deviceId) {
        int currentFailed = Optional.ofNullable(accountEntity.getFailedLoginCount()).orElse(0);
        int newCount = currentFailed + 1;
        boolean lockTriggered = newCount >= MAX_FAILED_ATTEMPTS;
        LocalDateTime lockedUntil = lockTriggered ? LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES) : null;
        if (lockTriggered) {
            userAccountMapper.lockAccount(accountEntity.getId(), newCount, lockedUntil);
            audit(AuditAction.ACCOUNT_LOCKED, accountEntity.getId(), deviceId,
                    "/api/v1/auth/login", "POST", "account_locked_after_" + newCount + "_failures");
        } else {
            userAccountMapper.incrementFailedLoginCount(accountEntity.getId());
        }
        recordLoginAttempt(accountEntity.getId(), accountEntity.getUsername(), false, deviceId, lockTriggered);
    }

    private void recordLoginAttempt(Long userId, String usernameAttempt, boolean success,
                                    String deviceId, boolean lockTriggered) {
        UserLoginRecordEntity record = new UserLoginRecordEntity();
        record.setUserId(userId);
        record.setUsernameAttempt(usernameAttempt);
        record.setSuccess(success);
        record.setDeviceId(deviceId);
        record.setLockTriggered(lockTriggered);
        userLoginRecordMapper.insert(record);
    }

    @Override
    public AccessTokenVO refresh(RefreshTokenRequest request) {
        AuthRefreshSession session = authSessionService.getRefreshSession(request.refreshToken())
                .orElseThrow(() -> new BusinessException(StandardErrorCode.UNAUTHENTICATED, "refresh token invalid"));

        if (!authSessionService.matchesDevice(session, request.deviceId())) {
            audit(AuditAction.AUTH_DEVICE_MISMATCH, session.userId(), request.deviceId(),
                    "/api/v1/auth/refresh", "POST", "refresh_device_mismatch");
            throw new BusinessException(StandardErrorCode.UNAUTHENTICATED, "refresh token invalid");
        }

        UserAccountEntity accountEntity = userAccountMapper.findById(session.userId());
        if (accountEntity == null || accountEntity.getStatus() != UserStatus.NORMAL) {
            throw new BusinessException(StandardErrorCode.ACCOUNT_DISABLED);
        }
        if (!Objects.equals(accountEntity.getLoginVersion(), session.loginVersion())) {
            authSessionService.revokeRefreshToken(request.refreshToken());
            audit(AuditAction.TOKEN_REFRESH, session.userId(), request.deviceId(),
                    "/api/v1/auth/refresh", "POST", "refresh_login_version_mismatch");
            throw new BusinessException(StandardErrorCode.UNAUTHENTICATED, "refresh token invalid");
        }

        AccessTokenVO accessTokenVO = authSessionService.rotateTokens(request.refreshToken(), toDomain(accountEntity), request.deviceId());
        audit(AuditAction.TOKEN_REFRESH, accountEntity.getId(), request.deviceId(),
                "/api/v1/auth/refresh", "POST", "refresh_success");
        return accessTokenVO;
    }

    @Override
    public void logout() {
        Long userId = CurrentUserHolder.get()
                .map(currentUser -> currentUser.userId())
                .orElse(null);
        String deviceId = CurrentUserHolder.get()
                .map(currentUser -> currentUser.deviceId())
                .orElse(null);
        authSessionService.revokeCurrentSession(CurrentUserHolder.get()
                .orElseThrow(() -> new BusinessException(StandardErrorCode.UNAUTHENTICATED)));
        audit(AuditAction.LOGOUT, userId, deviceId, "/api/v1/auth/logout", "POST", "logout_success");
    }

    private UserAccount toDomain(UserAccountEntity entity) {
        Set<UserRole> roles = parseRolesSafely(entity.getRoles());
        return new UserAccount(
                entity.getId(),
                entity.getUsername(),
                entity.getPasswordHash(),
                entity.getStatus(),
                roles,
                Optional.ofNullable(entity.getLoginVersion()).orElse(0L),
                entity.getCreatedAt());
    }

    private boolean passwordMatches(String rawPassword, String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) {
            return false;
        }
        try {
            return passwordEncoder.matches(rawPassword, encodedPassword);
        } catch (IllegalArgumentException ex) {
            log.warn("Password hash format is incompatible with BCrypt. reject login.");
            return false;
        }
    }

    private void audit(AuditAction action,
                       Long userId,
                       String deviceId,
                       String path,
                       String method,
                       String detail) {
        securityAuditService.record(new SecurityAuditEvent(
                action,
                userId,
                deviceId,
                path,
                method,
                TraceIdHolder.get().orElse(""),
                detail));
    }

    private Set<UserRole> parseRolesSafely(String rawRoles) {
        try {
            return DelimitedValueCodec.parseUserRoles(rawRoles);
        } catch (RuntimeException ex) {
            log.debug("Invalid role payload in user_account.roles, fallback to USER.");
            return Set.of(UserRole.USER);
        }
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Long userId = CurrentUserHolder.get()
                .map(cu -> cu.userId())
                .orElseThrow(() -> new BusinessException(StandardErrorCode.UNAUTHENTICATED));
        UserAccountEntity accountEntity = userAccountMapper.findById(userId);
        if (accountEntity == null) {
            throw new BusinessException(StandardErrorCode.UNAUTHENTICATED);
        }
        if (!passwordMatches(request.currentPassword(), accountEntity.getPasswordHash())) {
            throw new BusinessException(StandardErrorCode.BUSINESS_STATE_INVALID, "current password is incorrect");
        }
        String newHash = passwordEncoder.encode(request.newPassword());
        userAccountMapper.updatePassword(userId, newHash);
        userAccountMapper.incrementLoginVersion(userId);
        audit(AuditAction.PASSWORD_CHANGE, userId, null, "/api/v1/users/me/password", "PUT", "password_changed");
    }

    @Override
    public PageResponse<LoginRecordVO> queryLoginRecords(int page, int size) {
        Long userId = CurrentUserHolder.get()
                .map(cu -> cu.userId())
                .orElseThrow(() -> new BusinessException(StandardErrorCode.UNAUTHENTICATED));
        int offset = (page - 1) * size;
        long total = userLoginRecordMapper.countByUser(userId);
        List<LoginRecordVO> records = userLoginRecordMapper.queryByUser(userId, offset, size)
                .stream()
                .map(e -> new LoginRecordVO(
                        e.getId(),
                        Boolean.TRUE.equals(e.getSuccess()),
                        e.getIpAddress(),
                        e.getDeviceId(),
                        Boolean.TRUE.equals(e.getLockTriggered()),
                        e.getCreatedAt()))
                .toList();
        return PageResponse.of(records, total, page, size);
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
