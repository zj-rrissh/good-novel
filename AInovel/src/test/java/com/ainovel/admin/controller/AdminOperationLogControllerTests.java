package com.ainovel.admin.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AdminOperationLogControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from admin_operation_log");
    }

    @Test
    void shouldQueryOperationLogsWithFiltersFromAdminPath() throws Exception {
        insertOperationLog("NOVEL_BAN", "NOVEL", 101L, 9001L, "trace-ban-1", "/api/admin/v1/novels/101/ban",
                LocalDateTime.of(2026, 3, 26, 10, 0));
        insertOperationLog("COMMENT_HIDE", "COMMENT", 201L, 9002L, "trace-hide-1", "/api/admin/v1/community/comments/201/hide",
                LocalDateTime.of(2026, 3, 26, 11, 0));

        mockMvc.perform(get("/api/admin/v1/operation-logs")
                        .header("X-Client", "admin")
                        .param("action", "COMMENT_HIDE")
                        .param("bizType", "COMMENT")
                        .param("operatorId", "9002")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].action").value("COMMENT_HIDE"))
                .andExpect(jsonPath("$.data.records[0].bizType").value("COMMENT"))
                .andExpect(jsonPath("$.data.records[0].bizId").value(201))
                .andExpect(jsonPath("$.data.records[0].operatorId").value(9002))
                .andExpect(jsonPath("$.data.records[0].traceId").value("trace-hide-1"));
    }

    @Test
    void shouldQueryOperationLogsFromCompatiblePathSortedByCreatedAtDesc() throws Exception {
        insertOperationLog("NOVEL_BAN", "NOVEL", 301L, 9101L, "trace-ban-old", "/api/v1/admin/novels/301/ban",
                LocalDateTime.of(2026, 3, 26, 9, 0));
        insertOperationLog("AUDIT_MANUAL_DECIDED", "AUDIT_TASK", 401L, 9101L, "trace-audit-new",
                "/api/v1/admin/audit/tasks/401/review",
                LocalDateTime.of(2026, 3, 26, 12, 0));

        mockMvc.perform(get("/api/v1/admin/operation-logs")
                        .header("X-Client", "admin")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[0].action").value("AUDIT_MANUAL_DECIDED"))
                .andExpect(jsonPath("$.data.records[1].action").value("NOVEL_BAN"));
    }

    private void insertOperationLog(String action,
                                    String bizType,
                                    Long bizId,
                                    Long operatorId,
                                    String traceId,
                                    String requestPath,
                                    LocalDateTime createdAt) {
        jdbcTemplate.update(
                """
                insert into admin_operation_log (
                    action, biz_type, biz_id, operator_id, operator_roles, from_status, to_status,
                    reason, trace_id, request_path, created_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                action,
                bizType,
                bizId,
                operatorId,
                "ADMIN",
                "PENDING",
                "PASS",
                "reason",
                traceId,
                requestPath,
                Timestamp.valueOf(createdAt));
    }
}
