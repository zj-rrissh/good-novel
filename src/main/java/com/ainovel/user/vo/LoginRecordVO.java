package com.ainovel.user.vo;

import java.time.LocalDateTime;

public record LoginRecordVO(
        Long id,
        boolean success,
        String ipAddress,
        String deviceId,
        boolean lockTriggered,
        LocalDateTime createdAt
) {
}
