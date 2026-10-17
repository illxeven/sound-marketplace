package com.soundmarketplace.sound_marketplace;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

public class FlywayMigrationIT extends MySqlIntegrationTest {

    @Autowired(required = false)
    private Flyway flyway;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Empty DB migrates V1->V3 cleanly and ddl-auto=validate passes")
    void testFlywayMigrationV1ThroughV3() throws Exception {
        if (flyway != null) {
            MigrationInfo[] applied = flyway.info().applied();
            assertTrue(applied.length >= 3, "Expected at least 3 Flyway migrations applied");
            for (MigrationInfo info : applied) {
                assertTrue(info.getState().isApplied(), "Migration " + info.getVersion() + " should be applied");
            }
        }

        // Verify key tables created in V2 and V3 exist in database
        try (Connection conn = dataSource.getConnection()) {
            try (ResultSet rs = conn.getMetaData().getTables(null, null, "orders", null)) {
                assertTrue(rs.next(), "Table 'orders' must exist");
            }
            try (ResultSet rs = conn.getMetaData().getTables(null, null, "entitlements", null)) {
                assertTrue(rs.next(), "Table 'entitlements' must exist");
            }
            try (ResultSet rs = conn.getMetaData().getTables(null, null, "refresh_tokens", null)) {
                assertTrue(rs.next(), "Table 'refresh_tokens' must exist");
            }
        }
    }
}
