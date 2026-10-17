package com.soundmarketplace.sound_marketplace;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class FlywayUpgradeIT extends MySqlIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("V2 cleans duplicate relationships and orphan comment parents on upgraded schema")
    void testV2CleansLegacyDataAndAppliesConstraints() throws Exception {
        // Build clean test database targeting V1 only
        Flyway v1Flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("1")
                .cleanDisabled(false)
                .load();

        v1Flyway.clean();
        v1Flyway.migrate();

        // Insert legacy fixture: test users, duplicate follows, and orphan parent comment
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Create test users
            stmt.executeUpdate("INSERT INTO user_account (id, email, password_hash, is_active) VALUES " +
                    "(1001, 'user1@test.com', 'hash1', 1), " +
                    "(1002, 'user2@test.com', 'hash2', 1)");

            // Create duplicate follows (r1.id > r2.id)
            stmt.executeUpdate("INSERT INTO user_relationships (id, follower_id, followed_id, created_at) VALUES " +
                    "(100, 1001, 1002, NOW()), " +
                    "(101, 1001, 1002, NOW())");

            // Create a test track
            stmt.executeUpdate("INSERT INTO tracks (id, title, file_path) VALUES " +
                    "(2001, 'Test Upgrade Track', 's3://bucket/test.wav')");

            // Create comment with non-existent parent_comment_id (orphan)
            stmt.executeUpdate("INSERT INTO track_comments (id, user_id, track_id, comment_text, parent_comment_id) VALUES " +
                    "(3001, 1001, 2001, 'Orphan comment', 999999)");
        }

        // Run Flyway V2 migration
        Flyway v2Flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("2")
                .load();

        v2Flyway.migrate();

        // Verify that duplicates were removed from user_relationships
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM user_relationships WHERE follower_id = 1001 AND followed_id = 1002")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1), "Duplicate follow should be removed, leaving exactly 1");
            }

            // Verify orphan parent comment was set to NULL
            try (ResultSet rs = stmt.executeQuery("SELECT parent_comment_id FROM track_comments WHERE id = 3001")) {
                assertTrue(rs.next());
                assertNull(rs.getObject(1), "Orphan parent_comment_id should be set to NULL");
            }

            // Verify unique key uk_follow exists
            try (ResultSet rs = conn.getMetaData().getIndexInfo(null, null, "user_relationships", true, false)) {
                boolean foundUniqueFollow = false;
                while (rs.next()) {
                    String indexName = rs.getString("INDEX_NAME");
                    if ("uk_follow".equalsIgnoreCase(indexName)) {
                        foundUniqueFollow = true;
                        break;
                    }
                }
                assertTrue(foundUniqueFollow, "Unique key 'uk_follow' should exist");
            }
        }
    }
}
