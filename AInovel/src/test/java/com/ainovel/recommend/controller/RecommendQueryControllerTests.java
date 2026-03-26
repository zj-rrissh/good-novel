package com.ainovel.recommend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class RecommendQueryControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        CurrentUserHolder.clear();
        jdbcTemplate.update("delete from reading_progress");
        jdbcTemplate.update("delete from reaction");
        jdbcTemplate.update("delete from novel_chapter");
        jdbcTemplate.update("delete from novel");
        jdbcTemplate.update("delete from user_profile");
        jdbcTemplate.update("delete from user_account");

        insertUser(901L, "author_a", "Author A");
        insertUser(902L, "author_b", "Author B");
        insertUser(903L, "reader_1", "Reader One");
        insertUser(904L, "peer_1", "Peer One");

        insertNovel(1001L, 901L, "Home Champion", 10L, 280_000L, "ON_SHELF");
        insertNovel(1002L, 902L, "Home Runner", 10L, 160_000L, "ON_SHELF");
        insertNovel(1003L, 901L, "Quiet Shelf", 11L, 80_000L, "ON_SHELF");
        insertNovel(2001L, 901L, "Anchor Novel", 77L, 300_000L, "ON_SHELF");
        insertNovel(2002L, 902L, "Same Category Match", 77L, 220_000L, "ON_SHELF");
        insertNovel(2003L, 902L, "Cross Category Backup", 99L, 190_000L, "ON_SHELF");
        insertNovel(3001L, 901L, "Hidden Draft", 10L, 210_000L, "DRAFT");

        insertChapter(11001L, 1001L);
        insertChapter(11002L, 1002L);

        insertReaction("LIKE", "NOVEL", 1001L, 903L);
        insertReaction("LIKE", "NOVEL", 1001L, 904L);
        insertReaction("FAVORITE", "NOVEL", 1001L, 903L);
        insertReaction("LIKE", "NOVEL", 1002L, 903L);

        insertReadingProgress(903L, 1001L, 11001L, LocalDateTime.of(2026, 3, 25, 9, 0));
        insertReadingProgress(903L, 1002L, 11002L, LocalDateTime.of(2026, 3, 26, 9, 0));
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    @Test
    void shouldReturnHomeRecommendationsFromRealDatabaseData() throws Exception {
        mockMvc.perform(get("/api/v1/recommend/home")
                        .header("X-Client", "pc-web")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.scene").value("HOME"))
                .andExpect(jsonPath("$.data.items.length()").value(3))
                .andExpect(jsonPath("$.data.items[0].title").value("Home Champion"))
                .andExpect(jsonPath("$.data.items[0].authorName").value("Author A"))
                .andExpect(jsonPath("$.data.items[0].novelId").value(1001))
                .andExpect(jsonPath("$.data.items[?(@.title=='sample novel')]").isEmpty());
    }

    @Test
    void shouldReturnRelatedRecommendationsWithoutCurrentNovel() throws Exception {
        mockMvc.perform(get("/api/v1/recommend/novels/{novelId}/related", 2001)
                        .header("X-Client", "pc-web")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.scene").value("RELATED"))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].novelId").value(2002))
                .andExpect(jsonPath("$.data.items[?(@.novelId==2001)]").isEmpty());
    }

    @Test
    void shouldReturnEmptyContinueListForAnonymousUsers() throws Exception {
        CurrentUserHolder.clear();

        mockMvc.perform(get("/api/v1/recommend/continue")
                        .header("X-Client", "pc-web")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.scene").value("CONTINUE"))
                .andExpect(jsonPath("$.data.degradeLevel").value("LEVEL_2"))
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    void shouldReturnContinueRecommendationsForLoggedInUser() throws Exception {
        CurrentUserHolder.set(new CurrentUser(
                903L,
                Set.of(RoleType.USER),
                "device-903",
                true,
                "token-903",
                1L,
                Instant.now().plusSeconds(600)));

        mockMvc.perform(get("/api/v1/recommend/continue")
                        .header("X-Client", "pc-web")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].novelId").value(1002))
                .andExpect(jsonPath("$.data.items[1].novelId").value(1001));
    }

    private void insertUser(Long userId, String username, String nickname) {
        jdbcTemplate.update(
                """
                insert into user_account (id, username, password_hash, status, roles, login_version, created_at, updated_at)
                values (?, ?, ?, 'NORMAL', 'USER', 0, current_timestamp, current_timestamp)
                """,
                userId,
                username,
                "$2a$10$QJjYyx7y2du2Q1Wh0Lsp8usHfT3a6ICoJ56Q8M.8hM6otd2xTv2pu");
        jdbcTemplate.update(
                """
                insert into user_profile (user_id, nickname, avatar_url, bio, level, verified_status, created_at, updated_at)
                values (?, ?, null, null, 1, 'UNVERIFIED', current_timestamp, current_timestamp)
                """,
                userId,
                nickname);
    }

    private void insertNovel(Long novelId,
                             Long authorId,
                             String title,
                             Long categoryId,
                             Long wordCount,
                             String status) {
        jdbcTemplate.update(
                """
                insert into novel (
                    id, author_id, title, intro, cover_url, category_id, tag_ids, status,
                    latest_chapter_id, word_count, audit_task_id, created_at, updated_at
                ) values (?, ?, ?, 'intro', ?, ?, ?, ?, null, ?, null, current_timestamp, current_timestamp)
                """,
                novelId,
                authorId,
                title,
                "https://example.com/" + novelId + ".png",
                categoryId,
                null,
                status,
                wordCount);
    }

    private void insertChapter(Long chapterId, Long novelId) {
        jdbcTemplate.update(
                """
                insert into novel_chapter (id, novel_id, chapter_no, title, content, status, audit_task_id,
                                           published_at, created_at, updated_at)
                values (?, ?, 1, ?, 'content', 'PUBLISHED', null, current_timestamp, current_timestamp, current_timestamp)
                """,
                chapterId,
                novelId,
                "chapter-" + chapterId);
    }

    private void insertReaction(String reactionType, String targetType, Long targetId, Long userId) {
        jdbcTemplate.update(
                """
                insert into reaction (reaction_type, target_type, target_id, user_id, status, created_at, updated_at)
                values (?, ?, ?, ?, 'ACTIVE', current_timestamp, current_timestamp)
                """,
                reactionType,
                targetType,
                targetId,
                userId);
    }

    private void insertReadingProgress(Long userId,
                                       Long novelId,
                                       Long chapterId,
                                       LocalDateTime updatedAt) {
        jdbcTemplate.update(
                """
                insert into reading_progress (user_id, novel_id, chapter_id, progress_percent, page_offset, created_at, updated_at)
                values (?, ?, ?, 45, 1024, ?, ?)
                """,
                userId,
                novelId,
                chapterId,
                Timestamp.valueOf(updatedAt.minusMinutes(1)),
                Timestamp.valueOf(updatedAt));
    }
}
