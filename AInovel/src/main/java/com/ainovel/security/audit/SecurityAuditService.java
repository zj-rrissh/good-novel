package com.ainovel.security.audit;

public interface SecurityAuditService {

    void record(SecurityAuditEvent event);
}
