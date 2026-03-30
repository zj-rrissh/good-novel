package com.ainovel.user.entity;

import java.time.LocalDateTime;

public class UserLoginRecordEntity {

    private Long id;
    private Long userId;
    private String usernameAttempt;
    private Boolean success;
    private String ipAddress;
    private String deviceId;
    private Boolean lockTriggered;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsernameAttempt() {
        return usernameAttempt;
    }

    public void setUsernameAttempt(String usernameAttempt) {
        this.usernameAttempt = usernameAttempt;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Boolean getLockTriggered() {
        return lockTriggered;
    }

    public void setLockTriggered(Boolean lockTriggered) {
        this.lockTriggered = lockTriggered;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
