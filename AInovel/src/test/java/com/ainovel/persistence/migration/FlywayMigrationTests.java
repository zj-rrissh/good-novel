package com.ainovel.persistence.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:ainoveltest_flyway;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class FlywayMigrationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @Test
    void shouldCreateAllCoreBusinessTables() {
        String[] expectedTables = new String[] {
                "user_account",
                "user_profile",
                "user_message",
                "novel",
                "novel_chapter",
                "comment",
                "reaction",
                "user_follow",
                "audit_task",
                "reading_progress",
                "bookmark",
                "reading_history"
        };
        for (String table : expectedTables) {
            Integer tableCount = jdbcTemplate.queryForObject(
                    "select count(*) from INFORMATION_SCHEMA.TABLES where lower(TABLE_NAME) = ?",
                    Integer.class,
                    table);
            assertEquals(1, tableCount, "missing table: " + table);
        }
    }

    @Test
    void shouldCreateFlywayHistoryAndApplyInitialSchemaMigration() {
        Integer tableCount = jdbcTemplate.queryForObject(
                "select count(*) from INFORMATION_SCHEMA.TABLES where lower(TABLE_NAME) = 'flyway_schema_history'",
                Integer.class);
        assertEquals(1, tableCount);

        MigrationInfo current = flyway.info().current();
        assertTrue(current != null && current.getVersion() != null);
        assertEquals("2", current.getVersion().getVersion());
    }

    @Test
    void shouldTrackRepeatableSeedMigrationAndSeedDemoUsers() {
        boolean hasSeedRepeatable = false;
        for (MigrationInfo migrationInfo : flyway.info().applied()) {
            if (migrationInfo.getVersion() == null
                    && migrationInfo.getDescription() != null
                    && migrationInfo.getDescription().toLowerCase().contains("seed demo data")) {
                hasSeedRepeatable = true;
                break;
            }
        }
        assertTrue(hasSeedRepeatable);

        Long demoUserCount = jdbcTemplate.queryForObject(
                """
                        select count(*) from user_account
                        where username in ('reader_demo', 'author_demo', 'admin_demo')
                        """,
                Long.class);
        assertEquals(3L, demoUserCount);
    }

    @Test
    void shouldKeepDemoSeedsStableAfterRepeatedMigrate() {
        flyway.migrate();
        flyway.migrate();

        Long demoUserCount = jdbcTemplate.queryForObject(
                """
                        select count(*) from user_account
                        where id in (1001, 1002, 1003)
                        """,
                Long.class);
        assertEquals(3L, demoUserCount);
    }
}
