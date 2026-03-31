package com.ainovel.user.vo;

import com.ainovel.user.domain.UserRole;
import com.ainovel.user.domain.UserStatus;
import java.util.Set;

public record UserMeVO(Long userId, String username, UserStatus status, Set<UserRole> roles, UserProfileVO profile) {
}
