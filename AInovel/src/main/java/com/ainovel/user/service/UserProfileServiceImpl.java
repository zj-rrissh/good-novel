package com.ainovel.user.service;

import com.ainovel.cache.key.CacheKeyFactory;
import com.ainovel.cache.support.CacheTtlLevel;
import com.ainovel.cache.support.UnifiedCacheManager;
import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.persistence.support.DelimitedValueCodec;
import com.ainovel.security.auth.context.CurrentUserHolder;
import com.ainovel.user.domain.UserAccount;
import com.ainovel.user.entity.UserAccountEntity;
import com.ainovel.user.entity.UserProfileEntity;
import com.ainovel.user.mapper.UserAccountMapper;
import com.ainovel.user.mapper.UserProfileMapper;
import com.ainovel.user.dto.UpdateUserProfileRequest;
import com.ainovel.user.vo.UserMeVO;
import com.ainovel.user.vo.UserProfileVO;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileServiceImpl implements UserProfileService {

    private final UserAccountMapper userAccountMapper;
    private final UserProfileMapper userProfileMapper;
    private final UnifiedCacheManager unifiedCacheManager;
    private final CacheKeyFactory cacheKeyFactory;

    public UserProfileServiceImpl(UserAccountMapper userAccountMapper,
                                  UserProfileMapper userProfileMapper,
                                  UnifiedCacheManager unifiedCacheManager,
                                  CacheKeyFactory cacheKeyFactory) {
        this.userAccountMapper = userAccountMapper;
        this.userProfileMapper = userProfileMapper;
        this.unifiedCacheManager = unifiedCacheManager;
        this.cacheKeyFactory = cacheKeyFactory;
    }

    @Override
    public UserMeVO currentUser() {
        Long userId = currentUserId();
        UserAccount account = loadAccount(userId);
        UserProfileVO profile = loadProfile(userId);
        return new UserMeVO(account.id(), account.username(), account.status(), account.roles(), profile);
    }

    @Override
    @Transactional
    public UserProfileVO updateProfile(UpdateUserProfileRequest request) {
        Long userId = currentUserId();
        UserProfileEntity existing = userProfileMapper.findByUserId(userId);
        if (existing == null) {
            throw new BusinessException(StandardErrorCode.INVALID_REQUEST, "user profile not found");
        }

        UserProfileEntity updateEntity = new UserProfileEntity();
        updateEntity.setUserId(userId);
        updateEntity.setNickname(normalizeProfileField(request.nickname(), existing.getNickname()));
        updateEntity.setAvatarUrl(normalizeProfileField(request.avatarUrl(), existing.getAvatarUrl()));
        updateEntity.setBio(normalizeProfileField(request.bio(), existing.getBio()));

        return unifiedCacheManager.writeDbThenInvalidate(cacheKeyFactory.userProfile(userId), () -> {
            userProfileMapper.update(updateEntity);
            UserProfileEntity updated = userProfileMapper.findByUserId(userId);
            return toProfileVO(updated);
        });
    }

    private UserAccount loadAccount(Long userId) {
        UserAccountEntity entity = userAccountMapper.findById(userId);
        if (entity == null) {
            throw new BusinessException(StandardErrorCode.UNAUTHENTICATED);
        }
        return new UserAccount(
                entity.getId(),
                entity.getUsername(),
                entity.getPasswordHash(),
                entity.getStatus(),
                DelimitedValueCodec.parseUserRoles(entity.getRoles()),
                Optional.ofNullable(entity.getLoginVersion()).orElse(0L),
                entity.getCreatedAt());
    }

    private UserProfileVO loadProfile(Long userId) {
        String cacheKey = cacheKeyFactory.userProfile(userId);
        return unifiedCacheManager.readThrough(
                        cacheKey,
                        UserProfileVO.class,
                        CacheTtlLevel.DETAIL,
                        () -> Optional.ofNullable(userProfileMapper.findByUserId(userId)).map(this::toProfileVO))
                .orElseThrow(() -> new BusinessException(StandardErrorCode.INVALID_REQUEST, "user profile not found"));
    }

    private UserProfileVO toProfileVO(UserProfileEntity entity) {
        return new UserProfileVO(
                entity.getNickname(),
                entity.getAvatarUrl(),
                entity.getBio(),
                Optional.ofNullable(entity.getLevel()).orElse(1),
                entity.getVerifiedStatus());
    }

    private Long currentUserId() {
        return CurrentUserHolder.get()
                .map(currentUser -> currentUser.userId())
                .orElseThrow(() -> new BusinessException(StandardErrorCode.UNAUTHENTICATED));
    }

    private String normalizeProfileField(String requestValue, String defaultValue) {
        return requestValue == null ? defaultValue : requestValue.trim();
    }
}
