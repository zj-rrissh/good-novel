package com.ainovel.cache.key;

import com.ainovel.cache.support.UnifiedCacheManager;
import org.springframework.stereotype.Component;

@Component
public class CacheKeyFactory {

    private final UnifiedCacheManager unifiedCacheManager;

    public CacheKeyFactory(UnifiedCacheManager unifiedCacheManager) {
        this.unifiedCacheManager = unifiedCacheManager;
    }

    public String userProfile(Long userId) {
        return unifiedCacheManager.key(CacheKeyPrefixes.USER_PROFILE, userId);
    }

    public String novelDetail(Long novelId) {
        return unifiedCacheManager.key(CacheKeyPrefixes.NOVEL_DETAIL, novelId);
    }

    public String novelChapters(Long novelId) {
        return unifiedCacheManager.key(CacheKeyPrefixes.NOVEL_CHAPTERS, novelId);
    }

    public String chapterContent(Long chapterId) {
        return unifiedCacheManager.key(CacheKeyPrefixes.CHAPTER_CONTENT, chapterId);
    }

    public String commentCount(String targetType, Long targetId) {
        return unifiedCacheManager.key(CacheKeyPrefixes.COMMENT_COUNT, targetType, targetId);
    }

    public String recommendHome(Long userId) {
        return unifiedCacheManager.key(CacheKeyPrefixes.RECOMMEND_HOME, userId);
    }

    public String recommendHomeAnonymous() {
        return unifiedCacheManager.key(CacheKeyPrefixes.RECOMMEND_HOME, "anon");
    }

    public String auditTask(Long taskId) {
        return unifiedCacheManager.key(CacheKeyPrefixes.AUDIT_TASK, taskId);
    }

    public String authAccessToken(String jti) {
        return unifiedCacheManager.key(CacheKeyPrefixes.AUTH_ACCESS_TOKEN, jti);
    }

    public String authLoginVersion(Long userId) {
        return unifiedCacheManager.key(CacheKeyPrefixes.AUTH_LOGIN_VERSION, userId);
    }

    public String authTokenBlacklist(String jti) {
        return unifiedCacheManager.key(CacheKeyPrefixes.AUTH_TOKEN_BLACKLIST, jti);
    }

    public String authRefreshToken(String refreshToken) {
        return unifiedCacheManager.key(CacheKeyPrefixes.AUTH_REFRESH_TOKEN, refreshToken);
    }

    public String authRefreshTokenJti(String jti) {
        return unifiedCacheManager.key(CacheKeyPrefixes.AUTH_REFRESH_TOKEN_JTI, jti);
    }

    public String readingProgress(Long userId, Long novelId) {
        return unifiedCacheManager.key(CacheKeyPrefixes.READING_PROGRESS, userId, novelId);
    }
}
