package com.ainovel.persistence.migration;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
