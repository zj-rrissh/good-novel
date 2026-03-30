package com.ainovel.audit.service;

import com.ainovel.audit.event.AuditTaskSubmittedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AuditTaskSubmittedListener {

    private final AuditTaskExecutionService auditTaskExecutionService;

    public AuditTaskSubmittedListener(AuditTaskExecutionService auditTaskExecutionService) {
        this.auditTaskExecutionService = auditTaskExecutionService;
    }

    @Async("applicationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(AuditTaskSubmittedEvent event) {
        auditTaskExecutionService.execute(event.taskId());
    }
}
