package com.yourname.gtstracker.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseMigrationUpgradeTest {

    @TempDir
    Path tempDir;

    @Test
    void upgradesFromVersion1ToCurrent() throws Exception {
        String jdbcUrl = "jdbc:sqlite:" + tempDir.resolve("upgrade-from-v1.sqlite").toAbsolutePath();
        createSchemaAtVersion(jdbcUrl, 1);

        DatabaseManager manager = new DatabaseManager(jdbcUrl);
        manager.initialize();

        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement statement = conn.createStatement()) {
            assertEquals(3, queryInt(statement, "SELECT MAX(version) FROM schema_version"));
            assertEquals(1, queryInt(statement, "SELECT COUNT(*) FROM sqlite_master WHERE type='index' AND name='idx_listings_status_last_seen'"));
            assertEquals(1, queryInt(statement, "SELECT COUNT(*) FROM sqlite_master WHERE type='index' AND name='idx_pokemon_species_shiny_iv'"));
            assertTrue(manager.getMigrationStatusSummary().contains("schema v3/3"));
        }
    }

    @Test
    void upgradesFromVersion2ToCurrent() throws Exception {
        String jdbcUrl = "jdbc:sqlite:" + tempDir.resolve("upgrade-from-v2.sqlite").toAbsolutePath();
        createSchemaAtVersion(jdbcUrl, 2);

        DatabaseManager manager = new DatabaseManager(jdbcUrl);
        manager.initialize();

        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement statement = conn.createStatement()) {
            assertEquals(3, queryInt(statement, "SELECT MAX(version) FROM schema_version"));
            assertEquals(0, queryInt(statement, "SELECT COUNT(*) FROM sqlite_master WHERE type='index' AND name='idx_listings_status'"));
            assertEquals(1, queryInt(statement, "SELECT COUNT(*) FROM sqlite_master WHERE type='index' AND name='idx_item_name'"));
            assertTrue(manager.getMigrationStatusSummary().contains("ready"));
        }
    }

    private static void createSchemaAtVersion(String jdbcUrl, int version) throws Exception {
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE listings (
                    id TEXT PRIMARY KEY,
                    listing_type TEXT CHECK(listing_type IN ('POKEMON', 'ITEM')) NOT NULL,
                    seller TEXT NOT NULL,
                    price INTEGER NOT NULL,
                    first_seen INTEGER NOT NULL,
                    last_seen INTEGER NOT NULL,
                    status TEXT CHECK(status IN ('active', 'sold', 'expired', 'unknown')) DEFAULT 'active',
                    source_first TEXT CHECK(source_first IN ('chat', 'screen')) NOT NULL,
                    source_last TEXT CHECK(source_last IN ('chat', 'screen')) NOT NULL
                )
                """);
            stmt.executeUpdate("""
                CREATE TABLE pokemon_listings (
                    listing_id TEXT PRIMARY KEY,
                    species TEXT NOT NULL,
                    level INTEGER,
                    is_shiny BOOLEAN DEFAULT 0,
                    iv_hp INTEGER,
                    iv_atk INTEGER,
                    iv_def INTEGER,
                    iv_spatk INTEGER,
                    iv_spdef INTEGER,
                    iv_speed INTEGER,
                    iv_total INTEGER GENERATED ALWAYS AS (
                        COALESCE(iv_hp, 0) + COALESCE(iv_atk, 0) + COALESCE(iv_def, 0) +
                        COALESCE(iv_spatk, 0) + COALESCE(iv_spdef, 0) + COALESCE(iv_speed, 0)
                    ) STORED,
                    nature TEXT,
                    ability TEXT,
                    gender TEXT,
                    pokeball TEXT,
                    extra_data TEXT,
                    FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE
                )
                """);
            stmt.executeUpdate("""
                CREATE TABLE item_listings (
                    listing_id TEXT PRIMARY KEY,
                    item_name TEXT NOT NULL,
                    quantity INTEGER DEFAULT 1,
                    extra_data TEXT,
                    FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE
                )
                """);
            stmt.executeUpdate("CREATE INDEX idx_listings_status ON listings(status)");
            stmt.executeUpdate("CREATE INDEX idx_listings_type ON listings(listing_type)");
            stmt.executeUpdate("CREATE INDEX idx_listings_last_seen ON listings(last_seen)");
            stmt.executeUpdate("CREATE INDEX idx_pokemon_species ON pokemon_listings(species)");
            stmt.executeUpdate("CREATE INDEX idx_item_name ON item_listings(item_name)");
            stmt.executeUpdate("CREATE TABLE schema_version (version INTEGER PRIMARY KEY, name TEXT NOT NULL, applied_at INTEGER NOT NULL)");
            stmt.executeUpdate("INSERT INTO schema_version(version, name, applied_at) VALUES (1, 'baseline_schema', 1700000000000)");

            if (version >= 2) {
                stmt.executeUpdate("CREATE INDEX idx_listings_status_last_seen ON listings(status, last_seen DESC)");
                stmt.executeUpdate("CREATE INDEX idx_listings_type_status_last_seen ON listings(listing_type, status, last_seen DESC)");
                stmt.executeUpdate("CREATE INDEX idx_pokemon_species_shiny_iv ON pokemon_listings(species, is_shiny, iv_total)");
                stmt.executeUpdate("INSERT INTO schema_version(version, name, applied_at) VALUES (2, 'listing_indexes', 1700000001000)");
            }
        }
    }

    private static int queryInt(Statement stmt, String sql) throws Exception {
        try (ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
