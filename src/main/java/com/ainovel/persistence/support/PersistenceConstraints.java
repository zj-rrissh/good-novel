package com.ainovel.persistence.support;

import java.util.List;

public final class PersistenceConstraints {

    public static final List<String> REQUIRED_AUDIT_COLUMNS = List.of("created_at", "updated_at");
    public static final List<String> REQUIRED_STRONG_CONSISTENCY_SCENES = List.of(
            "account-security",
            "novel-status-transition",
            "audit-task-terminal-state");

    private PersistenceConstraints() {
    }
}
