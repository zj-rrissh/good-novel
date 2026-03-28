package com.ainovel.audit.engine;

import com.ainovel.audit.domain.AuditStatus;
import com.ainovel.audit.domain.AuditTask;
import com.ainovel.audit.domain.RiskLevel;
import org.springframework.stereotype.Component;

@Component
public class NovelIntroAuditExecutor implements AuditExecutor {

    @Override
    public AuditExecutionResult execute(AuditTask task) {
        if (task.contentSnapshot() == null || task.contentSnapshot().isBlank()) {
            return new AuditExecutionResult(
                    AuditStatus.MANUAL_REVIEW,
                    RiskLevel.HIGH,
                    "snapshot_empty",
                    "snapshot is empty");
        }
        return new AuditExecutionResult(
                AuditStatus.MANUAL_REVIEW,
                RiskLevel.MEDIUM,
                "phase1_async_manual_review",
                "phase 1 async backbone routes new tasks to manual review");
    }
}
