package com.ainovel.user.service.support;

public record AuthAccessSession(Long userId, long loginVersion, String deviceId) {
}
