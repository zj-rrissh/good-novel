package com.ainovel.user.service.support;

public record AuthRefreshSession(Long userId, long loginVersion, String jti, String deviceId) {
}
