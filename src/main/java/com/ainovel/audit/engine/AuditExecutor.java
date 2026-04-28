package com.ainovel.audit.engine;

import com.ainovel.audit.domain.AuditTask;

public interface AuditExecutor {

    AuditExecutionResult execute(AuditTask task);
}
