package com.yourname.gtstracker;

import com.yourname.gtstracker.database.DatabaseManager;
import com.yourname.gtstracker.ingest.ListingIngestionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngestionPersistenceSmokeTest {

    @TempDir
    Path tempDir;

    @Test
    void ingestChatMessageParsesAndPersistsListing() throws Exception {
        String jdbcUrl = "jdbc:sqlite:" + tempDir.resolve("smoke.sqlite").toAbsolutePath();
        DatabaseManager databaseManager = new DatabaseManager(jdbcUrl);
        databaseManager.initialize();

        ListingIngestionService ingestionService = new ListingIngestionService(databaseManager);
        var result = ingestionService.ingestChatMessage("[GTS] Ash listed Shiny Gengar Lv55 for $250000");

        assertTrue(result.isPresent());

        try (Statement stmt = databaseManager.getConnection().createStatement()) {
            assertEquals(1, query(stmt, "SELECT COUNT(*) FROM listings"));
            assertEquals(1, query(stmt, "SELECT COUNT(*) FROM pokemon_listings"));
        }
    }

    private int query(Statement stmt, String sql) throws Exception {
        try (ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
