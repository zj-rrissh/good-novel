package com.ainovel.reading.controller;

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
class ReadingProgressControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private DistributedLockService distributedLockService;

    @BeforeEach
    void setUp() throws Exception {
        CurrentUserHolder.clear();
        jdbcTemplate.update("delete from reading_progress");
        jdbcTemplate.update("delete from novel_chapter");
        jdbcTemplate.update("delete from novel");
        jdbcTemplate.update("delete from user_profile");
        jdbcTemplate.update("delete from user_account");

        insertUser(1201L, "reader_1201");
        insertNovel(6101L, 1201L);
        insertChapter(7101L, 6101L);

        CurrentUserHolder.set(new CurrentUser(
                1201L,
                Set.of(RoleType.USER),
                "device-1201",
                true,
                "token-1201",
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
    void shouldSaveAndReadProgressViaController() throws Exception {
        String payload = """
                {
                  "novelId": 6101,
                  "chapterId": 7101,
                  "progressPercent": 35,
                  "clientTs": 1711450000000,
                  "deviceId": "device-1201",
                  "pageOffset": 128
                }
                """;

        mockMvc.perform(post("/api/v1/reading-progress")
                        .header("X-Client", "pc-web")
                        .header("Idempotency-Key", "progress-save-1")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.novelId").value(6101))
                .andExpect(jsonPath("$.data.chapterId").value(7101))
                .andExpect(jsonPath("$.data.progressPercent").value(35));

        mockMvc.perform(get("/api/v1/reading-progress")
                        .header("X-Client", "pc-web")
                        .param("novelId", "6101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.novelId").value(6101))
                .andExpect(jsonPath("$.data.chapterId").value(7101))
                .andExpect(jsonPath("$.data.progressPercent").value(35));
    }

    @Test
    void shouldReturnUnauthenticatedWhenCurrentUserMissing() throws Exception {
        CurrentUserHolder.clear();

        mockMvc.perform(get("/api/v1/reading-progress")
                        .header("X-Client", "pc-web")
                        .param("novelId", "6101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2001))
                .andExpect(jsonPath("$.message").value("unauthenticated"));
    }

    @Test
    void shouldReturnValidationFailedWhenNovelIdIsNotPositive() throws Exception {
        mockMvc.perform(get("/api/v1/reading-progress")
                        .header("X-Client", "pc-web")
                        .param("novelId", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void shouldReturnIdempotentConflictForRepeatedIdempotencyKey() throws Exception {
        String payload = """
                {
                  "novelId": 6101,
                  "chapterId": 7101,
                  "progressPercent": 55,
                  "clientTs": 1711450000000,
                  "deviceId": "device-1201",
                  "pageOffset": 256
                }
                """;

        mockMvc.perform(post("/api/v1/reading-progress")
                        .header("X-Client", "pc-web")
                        .header("Idempotency-Key", "progress-idempotent-1")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/v1/reading-progress")
                        .header("X-Client", "pc-web")
                        .header("Idempotency-Key", "progress-idempotent-1")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.message").value("idempotent conflict"));
    }

    @Test
    void shouldReleaseIdempotencyKeyWhenBusinessExceptionOccurs() throws Exception {
        String invalidPayload = """
                {
                  "novelId": 6101,
                  "chapterId": 999999,
                  "progressPercent": 45,
                  "clientTs": 1711450000000,
                  "deviceId": "device-1201",
                  "pageOffset": 0
                }
                """;

        mockMvc.perform(post("/api/v1/reading-progress")
                        .header("X-Client", "pc-web")
                        .header("Idempotency-Key", "progress-release-1")
                        .contentType("application/json")
                        .content(invalidPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));

        mockMvc.perform(post("/api/v1/reading-progress")
                        .header("X-Client", "pc-web")
                        .header("Idempotency-Key", "progress-release-1")
                        .contentType("application/json")
                        .content(invalidPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));
    }

    @Test
    void shouldReturnLockAcquireFailedWhenLockIsContended() throws Exception {
        when(distributedLockService.tryLock(anyString(), any(), any(), any())).thenReturn(false);

        String payload = """
                {
                  "novelId": 6101,
                  "chapterId": 7101,
                  "progressPercent": 60,
                  "clientTs": 1711450000000,
                  "deviceId": "device-1201",
                  "pageOffset": 300
                }
                """;

        mockMvc.perform(post("/api/v1/reading-progress")
                        .header("X-Client", "pc-web")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3003))
                .andExpect(jsonPath("$.message").value("lock acquire failed"));
    }

    private void insertUser(Long userId, String username) {
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
                username + "-nick");
    }

    private void insertNovel(Long novelId, Long authorId) {
        jdbcTemplate.update(
                """
                insert into novel (id, author_id, title, intro, cover_url, category_id, tag_ids, status,
                                   latest_chapter_id, word_count, audit_task_id, created_at, updated_at)
                values (?, ?, ?, 'intro', null, null, null, 'ON_SHELF', null, 0, null, current_timestamp, current_timestamp)
                """,
                novelId,
                authorId,
                "novel-" + novelId);
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
}
