package com.ainovel.reading.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class BookmarkControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Long USER_ID = 3301L;
    private static final Long OTHER_USER_ID = 3302L;
    private static final Long NOVEL_ID = 9301L;
    private static final Long CHAPTER_ID = 9302L;

    @BeforeEach
    void setUp() {
        CurrentUserHolder.clear();
        jdbcTemplate.update("delete from bookmark");
        jdbcTemplate.update("delete from reading_history");
        jdbcTemplate.update("delete from reading_progress");
        jdbcTemplate.update("delete from novel_chapter");
        jdbcTemplate.update("delete from novel");
        jdbcTemplate.update("delete from user_profile");
        jdbcTemplate.update("delete from user_account");

        insertUser(USER_ID, "user_3301");
        insertUser(OTHER_USER_ID, "user_3302");
        insertNovel(NOVEL_ID, USER_ID);
        insertChapter(CHAPTER_ID, NOVEL_ID);

        setCurrentUser(USER_ID);
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    @Test
    void shouldAddBookmark() throws Exception {
        mockMvc.perform(post("/api/v1/bookmarks")
                        .header("X-Client", "pc-web").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"novelId":%d,"chapterId":%d,"pageOffset":10,"note":"great scene"}
                                """.formatted(NOVEL_ID, CHAPTER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.novelId").value(NOVEL_ID))
                .andExpect(jsonPath("$.data.chapterId").value(CHAPTER_ID))
                .andExpect(jsonPath("$.data.pageOffset").value(10))
                .andExpect(jsonPath("$.data.note").value("great scene"));

        Long count = jdbcTemplate.queryForObject(
                "select count(*) from bookmark where user_id=? and chapter_id=?",
                Long.class, USER_ID, CHAPTER_ID);
        assertEquals(1L, count);
    }

    @Test
    void shouldReturnExistingBookmarkOnDuplicate() throws Exception {
        // add once
        mockMvc.perform(post("/api/v1/bookmarks")
                        .header("X-Client", "pc-web").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"novelId":%d,"chapterId":%d,"pageOffset":0}
                                """.formatted(NOVEL_ID, CHAPTER_ID)))
                .andExpect(status().isOk());

        // add again — should return same bookmark, not error
        mockMvc.perform(post("/api/v1/bookmarks")
                        .header("X-Client", "pc-web").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"novelId":%d,"chapterId":%d,"pageOffset":5}
                                """.formatted(NOVEL_ID, CHAPTER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.chapterId").value(CHAPTER_ID));

        Long count = jdbcTemplate.queryForObject(
                "select count(*) from bookmark where user_id=?", Long.class, USER_ID);
        assertEquals(1L, count);
    }

    @Test
    void shouldRejectBookmarkWhenChapterNotBelongToNovel() throws Exception {
        Long otherNovelId = 9399L;
        insertNovel(otherNovelId, USER_ID);

        mockMvc.perform(post("/api/v1/bookmarks")
                        .header("X-Client", "pc-web").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"novelId":%d,"chapterId":%d,"pageOffset":0}
                                """.formatted(otherNovelId, CHAPTER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));
    }

    @Test
    void shouldDeleteBookmark() throws Exception {
        insertBookmark(USER_ID, NOVEL_ID, CHAPTER_ID);
        Long bookmarkId = jdbcTemplate.queryForObject(
                "select id from bookmark where user_id=? and chapter_id=?",
                Long.class, USER_ID, CHAPTER_ID);
        assertNotNull(bookmarkId);

        mockMvc.perform(delete("/api/v1/bookmarks/" + bookmarkId).header("X-Client", "pc-web"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        Long count = jdbcTemplate.queryForObject(
                "select count(*) from bookmark where id=?", Long.class, bookmarkId);
        assertEquals(0L, count);
    }

    @Test
    void shouldRejectDeleteOtherUserBookmark() throws Exception {
        insertBookmark(USER_ID, NOVEL_ID, CHAPTER_ID);
        Long bookmarkId = jdbcTemplate.queryForObject(
                "select id from bookmark where user_id=? and chapter_id=?",
                Long.class, USER_ID, CHAPTER_ID);

        // switch to other user
        setCurrentUser(OTHER_USER_ID);
        mockMvc.perform(delete("/api/v1/bookmarks/" + bookmarkId).header("X-Client", "pc-web"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3000));

        Long count = jdbcTemplate.queryForObject(
                "select count(*) from bookmark where id=?", Long.class, bookmarkId);
        assertEquals(1L, count);
    }

    @Test
    void shouldListBookmarks() throws Exception {
        insertBookmark(USER_ID, NOVEL_ID, CHAPTER_ID);

        mockMvc.perform(get("/api/v1/bookmarks").header("X-Client", "pc-web").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].chapterId").value(CHAPTER_ID));
    }

    @Test
    void shouldListBookmarksByNovel() throws Exception {
        insertBookmark(USER_ID, NOVEL_ID, CHAPTER_ID);

        mockMvc.perform(get("/api/v1/bookmarks")
                        .header("X-Client", "pc-web")
                        .param("novelId", NOVEL_ID.toString())
                        .param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].novelId").value(NOVEL_ID));
    }

    @Test
    void shouldReturn401WhenUnauthenticated() throws Exception {
        CurrentUserHolder.clear();
        mockMvc.perform(get("/api/v1/bookmarks").header("X-Client", "pc-web"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2001));
    }

    // --- helpers ---

    private void setCurrentUser(Long userId) {
        CurrentUserHolder.set(new CurrentUser(
                userId,
                Set.of(RoleType.USER),
                "device-" + userId,
                true,
                "token-" + userId,
                1L,
                Instant.now().plusSeconds(600)));
    }

    private void insertUser(Long userId, String username) {
        jdbcTemplate.update("""
                insert into user_account (id, username, password_hash, status, roles, login_version, created_at, updated_at)
                values (?, ?, '$2a$10$QJjYyx7y2du2Q1Wh0Lsp8usHfT3a6ICoJ56Q8M.8hM6otd2xTv2pu', 'NORMAL', 'USER', 0, current_timestamp, current_timestamp)
                """, userId, username);
        jdbcTemplate.update("""
                insert into user_profile (user_id, nickname, avatar_url, bio, level, verified_status, created_at, updated_at)
                values (?, ?, null, null, 1, 'UNVERIFIED', current_timestamp, current_timestamp)
                """, userId, username + "-nick");
    }

    private void insertNovel(Long novelId, Long authorId) {
        jdbcTemplate.update("""
                insert into novel (id, author_id, title, intro, cover_url, category_id, tag_ids, status,
                                   latest_chapter_id, word_count, audit_task_id, created_at, updated_at)
                values (?, ?, ?, 'intro', null, null, null, 'ON_SHELF', null, 0, null, current_timestamp, current_timestamp)
                """, novelId, authorId, "novel-" + novelId);
    }

    private void insertChapter(Long chapterId, Long novelId) {
        jdbcTemplate.update("""
                insert into novel_chapter (id, novel_id, chapter_no, title, content, status, created_at, updated_at)
                values (?, ?, 1, 'Chapter 1', 'content', 'PUBLISHED', current_timestamp, current_timestamp)
                """, chapterId, novelId);
    }

    private void insertBookmark(Long userId, Long novelId, Long chapterId) {
        jdbcTemplate.update("""
                insert into bookmark (user_id, novel_id, chapter_id, page_offset, note)
                values (?, ?, ?, 0, null)
                """, userId, novelId, chapterId);
    }
}


