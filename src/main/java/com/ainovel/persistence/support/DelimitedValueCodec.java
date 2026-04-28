package com.ainovel.persistence.support;

import com.ainovel.security.auth.rbac.RoleType;
import com.ainovel.user.domain.UserRole;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class DelimitedValueCodec {

    private DelimitedValueCodec() {
    }

    public static Set<UserRole> parseUserRoles(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(UserRole::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static String formatUserRoles(Set<UserRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return UserRole.USER.name();
        }
        return roles.stream()
                .sorted(Comparator.comparing(Enum::name))
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }

    public static Set<RoleType> toRoleTypes(Set<UserRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of(RoleType.USER);
        }
        return roles.stream()
                .map(role -> RoleType.valueOf(role.name()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static Set<Long> parseLongSet(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static String formatLongSet(Set<Long> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
                .filter(value -> value != null && value > 0)
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}
