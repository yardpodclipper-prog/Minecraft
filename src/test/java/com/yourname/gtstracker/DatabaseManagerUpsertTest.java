package com.yourname.gtstracker;

import com.yourname.gtstracker.database.DatabaseManager;
import com.yourname.gtstracker.database.models.DataSource;
import com.yourname.gtstracker.database.models.ListingStatus;
import com.yourname.gtstracker.database.models.PokemonListing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseManagerUpsertTest {

    @TempDir
    Path tempDir;

    @Test
    void upsertListingInsertsThenUpdatesSameId() throws Exception {
        String jdbcUrl = "jdbc:sqlite:" + tempDir.resolve("gts-test.sqlite").toAbsolutePath();
        DatabaseManager databaseManager = new DatabaseManager(jdbcUrl);
        databaseManager.initialize();

        PokemonListing listing = new PokemonListing();
        listing.setId("listing-1");
        listing.setSeller("Ash");
        listing.setSpecies("Gengar");
        listing.setLevel(55);
        listing.setPrice(250000);
        listing.setStatus(ListingStatus.ACTIVE);
        listing.setSourceFirst(DataSource.CHAT);
        listing.setSourceLast(DataSource.CHAT);
        listing.setFirstSeen(1000);
        listing.setLastSeen(1000);

        databaseManager.upsertListing(listing);

        listing.setPrice(275000);
        listing.setLastSeen(2000);
        databaseManager.upsertListing(listing);

        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            assertEquals(1, queryCount(stmt, "SELECT COUNT(*) FROM listings"));
            assertEquals(1, queryCount(stmt, "SELECT COUNT(*) FROM pokemon_listings"));
            assertEquals(275000, queryCount(stmt, "SELECT price FROM listings WHERE id = 'listing-1'"));
        }
    }

    private int queryCount(Statement stmt, String sql) throws Exception {
        try (ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
