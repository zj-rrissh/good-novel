package com.ainovel.user.vo;

import java.time.LocalDateTime;

public record AccessTokenVO(String accessToken, String refreshToken, LocalDateTime expireAt, long loginVersion) {
}
