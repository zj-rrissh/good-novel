package com.ainovel.cache.key;

public final class CacheKeyPrefixes {

    public static final String LOCK = "lock";
    public static final String IDEMPOTENT = "idempotent";

    public static final String USER_PROFILE = "user:profile";
    public static final String NOVEL_DETAIL = "novel:detail";
    public static final String NOVEL_CHAPTERS = "novel:chapters";
    public static final String CHAPTER_CONTENT = "chapter:content";
    public static final String COMMENT_COUNT = "counter:comment";
    public static final String RECOMMEND_HOME = "recommend:home";
    public static final String AUDIT_TASK = "audit:task";
    public static final String AUTH_ACCESS_TOKEN = "auth:access_token";
    public static final String AUTH_LOGIN_VERSION = "auth:login_version";
    public static final String AUTH_TOKEN_BLACKLIST = "auth:token_blacklist";
    public static final String AUTH_REFRESH_TOKEN = "auth:refresh_token";
    public static final String AUTH_REFRESH_TOKEN_JTI = "auth:refresh_token_jti";
    public static final String READING_PROGRESS = "reading:progress";

    private CacheKeyPrefixes() {
    }
}
