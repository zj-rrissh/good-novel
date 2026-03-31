package com.ainovel.community.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class CommunityPartitionControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from novel_chapter");
        jdbcTemplate.update("delete from novel_community_partition");
        jdbcTemplate.update("delete from novel");
    }

    @Test
    void shouldListActivePartitionsByNovel() throws Exception {
        insertNovel(9001L, 1002L, "partition-novel");
        insertPartition(9001L, "CHARACTER", "人物讨论", 20, "ACTIVE");
        insertPartition(9001L, "PLOT", "剧情讨论", 10, "ACTIVE");
        insertPartition(9001L, "OFF_TOPIC", "闲聊区", 30, "DISABLED");

        mockMvc.perform(get("/api/v1/novels/{novelId}/community/partitions", 9001L)
                        .header("X-Client", "pc-web"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].partitionKey").value("PLOT"))
                .andExpect(jsonPath("$.data[0].partitionName").value("剧情讨论"))
                .andExpect(jsonPath("$.data[1].partitionKey").value("CHARACTER"));
    }

    @Test
    void shouldReturnInvalidRequestWhenNovelNotExists() throws Exception {
        mockMvc.perform(get("/api/v1/novels/{novelId}/community/partitions", 999999L)
                        .header("X-Client", "pc-web"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("invalid request"));
    }

    private void insertNovel(Long novelId, Long authorId, String title) {
        jdbcTemplate.update(
                """
                        insert into novel (
                            id, author_id, title, intro, cover_url, category_id, tag_ids, status,
                            latest_chapter_id, word_count, audit_task_id, created_at, updated_at
                        ) values (?, ?, ?, 'intro', null, null, null, 'ON_SHELF', null, 0, null, current_timestamp, current_timestamp)
                        """,
                novelId,
                authorId,
                title);
    }

    private void insertPartition(Long novelId, String partitionKey, String partitionName, int sortOrder, String status) {
        jdbcTemplate.update(
                """
                        insert into novel_community_partition (
                            novel_id, partition_key, partition_name, sort_order, status, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                        """,
                novelId,
                partitionKey,
                partitionName,
                sortOrder,
                status);
    }
}
