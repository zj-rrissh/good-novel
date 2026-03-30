package com.ainovel.audit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ainovel.infrastructure.aop.lock.DistributedLockService;
import com.ainovel.security.auth.context.CurrentUser;
import com.ainovel.security.auth.context.CurrentUserHolder;
import com.ainovel.security.auth.rbac.RoleType;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AdminAuditControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private DistributedLockService distributedLockService;

    @BeforeEach
    void setUp() throws Exception {
        CurrentUserHolder.clear();
        jdbcTemplate.update("delete from audit_task");

        CurrentUserHolder.set(new CurrentUser(
                9101L,
                Set.of(RoleType.ADMIN),
                "admin-device",
                true,
                "admin-token",
                1L,
                Instant.now().plusSeconds(600)));

        when(distributedLockService.tryLock(anyString(), any(), any(), any())).thenReturn(true);
        doNothing().when(distributedLockService).unlock(anyString(), any());
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    @Test
    void shouldQueryPendingTasksFromAdminPath() throws Exception {
        insertAuditTask(3001L, "PENDING", "pending-hash");
        insertAuditTask(3002L, "PASS", "pass-hash");

        mockMvc.perform(get("/api/admin/v1/audit/tasks")
                        .header("X-Client", "admin")
                        .param("status", "PENDING")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].bizId").value(3001))
                .andExpect(jsonPath("$.data.records[0].auditStatus").value("PENDING"));
    }

    @Test
    void shouldReviewTaskFromCompatibleAdminPath() throws Exception {
        Long taskId = insertAuditTask(4001L, "PENDING", "review-hash");

        String payload = """
                {
                  "decision": "REJECT",
                  "rejectReasonCode": "RISK_TEXT",
                  "rejectReasonText": "manual review rejected"
                }
                """;

        mockMvc.perform(post("/api/v1/admin/audit/tasks/{taskId}/review", taskId)
                        .header("X-Client", "admin")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.taskId").value(taskId))
                .andExpect(jsonPath("$.data.auditStatus").value("REJECT"))
                .andExpect(jsonPath("$.data.reasonCode").value("RISK_TEXT"))
                .andExpect(jsonPath("$.data.reviewerId").value(9101));
    }

    @Test
    void shouldReturnValidationFailedWhenPageSizeOutOfRange() throws Exception {
        mockMvc.perform(get("/api/admin/v1/audit/tasks")
                        .header("X-Client", "admin")
                        .param("page", "1")
                        .param("size", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void shouldReturnInvalidRequestWhenReviewTaskMissing() throws Exception {
        String payload = """
                {
                  "decision": "PASS",
                  "rejectReasonCode": null,
                  "rejectReasonText": null
                }
                """;

        mockMvc.perform(post("/api/admin/v1/audit/tasks/{taskId}/review", 99999)
                        .header("X-Client", "admin")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("invalid request"));
    }

    @Test
    void shouldReturnLockAcquireFailedWhenReviewLockIsContended() throws Exception {
        Long taskId = insertAuditTask(5001L, "PENDING", "lock-hash");
        when(distributedLockService.tryLock(anyString(), any(), any(), any())).thenReturn(false);

        String payload = """
                {
                  "decision": "PASS",
                  "rejectReasonCode": null,
                  "rejectReasonText": null
                }
                """;

        mockMvc.perform(post("/api/admin/v1/audit/tasks/{taskId}/review", taskId)
                        .header("X-Client", "admin")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3003))
                .andExpect(jsonPath("$.message").value("lock acquire failed"));
    }

    private Long insertAuditTask(Long bizId, String status, String contentHash) {
        jdbcTemplate.update(
                """
                insert into audit_task (
                    biz_type, biz_id, content_snapshot, content_hash, audit_status, risk_level,
                    reason_code, reason_text, reviewer_id, retry_count, rule_version, reviewed_at,
                    created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, null, null, null, 0, 'v1', null, current_timestamp, current_timestamp)
                """,
                "BIZ_COMMENT",
                bizId,
                "snapshot-" + bizId,
                contentHash,
                status,
                "MEDIUM");
        return jdbcTemplate.queryForObject("select max(task_id) from audit_task", Long.class);
    }
}
