package com.ainovel.novel.controller;

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
class AdminNovelControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from novel");

        insertNovel(2001L, "星轨药铺", "银河治愈故事", 10L, "ON_SHELF", LocalDateTime.of(2026, 3, 10, 12, 0));
        insertNovel(2001L, "城市夜行", "迷雾追踪", 10L, "ON_SHELF", LocalDateTime.of(2026, 3, 9, 12, 0));
        insertNovel(2002L, "草稿故事", "包含星轨关键词", 11L, "DRAFT", LocalDateTime.of(2026, 3, 8, 12, 0));
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
