package com.ainovel.novel.controller;

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
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
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
class AdminNovelControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private DistributedLockService distributedLockService;

    @BeforeEach
    void setUp() throws Exception {
        CurrentUserHolder.clear();
        jdbcTemplate.update("delete from admin_operation_log");
        jdbcTemplate.update("delete from novel_chapter");
        jdbcTemplate.update("delete from novel");

        insertNovel(2001L, "星轨药铺", "银河治愈故事", 10L, "ON_SHELF", LocalDateTime.of(2026, 3, 10, 12, 0));
        insertNovel(2001L, "城市夜行", "迷雾追踪", 10L, "ON_SHELF", LocalDateTime.of(2026, 3, 9, 12, 0));
        insertNovel(2002L, "草稿故事", "包含星轨关键词", 11L, "DRAFT", LocalDateTime.of(2026, 3, 8, 12, 0));

        CurrentUserHolder.set(new CurrentUser(
                9301L,
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
    void shouldQueryRealNovelsWithFilterAndPagination() throws Exception {
        mockMvc.perform(get("/api/admin/v1/novels")
                        .header("X-Client", "admin")
                        .param("status", "ON_SHELF")
                        .param("keyword", "星轨")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].title").value("星轨药铺"))
                .andExpect(jsonPath("$.data.records[0].status").value("ON_SHELF"));

        mockMvc.perform(get("/api/admin/v1/novels")
                        .header("X-Client", "admin")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.records.length()").value(2))
                .andExpect(jsonPath("$.data.records[0].title").value("星轨药铺"))
                .andExpect(jsonPath("$.data.records[1].title").value("城市夜行"));
    }

    @Test
    void shouldRejectInvalidStatusValue() throws Exception {
        mockMvc.perform(get("/api/admin/v1/novels")
                        .header("X-Client", "admin")
                        .param("status", "ONLINE")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void shouldBanNovelAndWriteAdminOperationLog() throws Exception {
        Long novelId = jdbcTemplate.queryForObject("select id from novel where title = '星轨药铺'", Long.class);

        mockMvc.perform(post("/api/admin/v1/novels/{novelId}/ban", novelId)
                        .header("X-Client", "admin")
                        .header("X-Trace-Id", "trace-novel-ban-1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "reason": "违规内容"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/admin/v1/novels")
                        .header("X-Client", "admin")
                        .param("status", "BANNED")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].title").value("星轨药铺"));

        Integer operationLogCount = jdbcTemplate.queryForObject(
                """
                select count(1)
                from admin_operation_log
                where action = 'NOVEL_BAN'
                  and biz_type = 'NOVEL'
                  and biz_id = ?
                  and operator_id = 9301
                  and trace_id = 'trace-novel-ban-1'
                """,
                Integer.class,
                novelId);
        org.junit.jupiter.api.Assertions.assertEquals(1, operationLogCount);
    }

    private void insertNovel(Long authorId,
                             String title,
                             String intro,
                             Long categoryId,
                             String status,
                             LocalDateTime updatedAt) {
        jdbcTemplate.update(
                """
                insert into novel (
                    author_id, title, intro, cover_url, category_id, tag_ids, status, latest_chapter_id, word_count, audit_task_id
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                authorId,
                title,
                intro,
                "https://example.com/cover.png",
                categoryId,
                null,
                status,
                null,
                0L,
                null);
        jdbcTemplate.update("update novel set updated_at = ? where title = ?",
                Timestamp.valueOf(updatedAt),
                title);
    }
}
