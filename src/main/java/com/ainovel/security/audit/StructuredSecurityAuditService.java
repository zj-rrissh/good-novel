package com.ainovel.security.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class StructuredSecurityAuditService implements SecurityAuditService {

    private static final Logger log = LoggerFactory.getLogger(StructuredSecurityAuditService.class);

    @Override
    public void record(SecurityAuditEvent event) {
        if (event == null) {
            return;
        }
        log.info("security audit action={} userId={} deviceId={} path={} method={} traceId={} detail={}",
                event.action(),
                event.userId() == null ? "" : event.userId(),
                safe(event.deviceId()),
                safe(event.path()),
                safe(event.method()),
                safe(event.traceId()),
                safe(event.detail()));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
