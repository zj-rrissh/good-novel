package com.ainovel.security.auth.context;

import java.util.Optional;

public final class CurrentUserHolder {

    private static final ThreadLocal<CurrentUser> CURRENT_USER = new ThreadLocal<>();

    private CurrentUserHolder() {
    }

    public static void set(CurrentUser currentUser) {
        CURRENT_USER.set(currentUser);
    }

    public static Optional<CurrentUser> get() {
        return Optional.ofNullable(CURRENT_USER.get());
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
