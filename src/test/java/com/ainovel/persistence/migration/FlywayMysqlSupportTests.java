package com.ainovel.persistence.migration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.ServiceLoader;

import org.flywaydb.core.extensibility.Plugin;
import org.junit.jupiter.api.Test;

class FlywayMysqlSupportTests {

    @Test
    void shouldExposeMysqlFlywayPluginOnClasspath() {
        boolean mysqlPluginPresent = ServiceLoader.load(Plugin.class)
                .stream()
                .map(ServiceLoader.Provider::type)
                .map(Class::getName)
                .anyMatch(className -> className.toLowerCase(Locale.ROOT).contains("mysql"));

        assertTrue(mysqlPluginPresent, "Flyway MySQL plugin should be present on the runtime classpath");
    }

    @Test
    void shouldAvoidUnsupportedAddColumnIfNotExistsInMysqlMigrationScript() throws IOException {
        String v3MigrationScript = Files.readString(
                Path.of("src/main/resources/db/migration/mysql/V3__user_account_lockout_login_record.sql"));

        assertFalse(
                v3MigrationScript.toUpperCase(Locale.ROOT).contains("ADD COLUMN IF NOT EXISTS"),
                "MySQL migration script should not use `ADD COLUMN IF NOT EXISTS` because MySQL 8.4 rejects this syntax");
    }
}
