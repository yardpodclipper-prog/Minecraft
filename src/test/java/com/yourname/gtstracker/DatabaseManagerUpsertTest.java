package com.yourname.gtstracker;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatabaseManagerUpsertTest {

    @TempDir
    Path tempDir;

    private Object databaseManager;
    private Method upsertMethod;
    private String jdbcUrl;

    @BeforeEach
    void setup() throws Exception {
        Class<?> managerClass = Class.forName("com.yourname.gtstracker.DatabaseManager");

        Path dbFile = Files.createFile(tempDir.resolve("gts-test.sqlite"));
        jdbcUrl = "jdbc:sqlite:" + dbFile;

        Constructor<?> ctor = Arrays.stream(managerClass.getDeclaredConstructors())
                .filter(c -> c.getParameterCount() == 1 && c.getParameterTypes()[0] == String.class)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected DatabaseManager(String jdbcOrPath) constructor"));
        ctor.setAccessible(true);
        databaseManager = ctor.newInstance(jdbcUrl);

        upsertMethod = Arrays.stream(managerClass.getDeclaredMethods())
                .filter(m -> m.getName().toLowerCase(Locale.ROOT).contains("upsert"))
                .filter(m -> m.getParameterCount() == 1)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected single-argument upsert method"));
        upsertMethod.setAccessible(true);
    }

    @Test
    void insertThenUpdatePersistsSubtypeRows() throws Exception {
        Map<String, Object> listing = pokemonListing("fp-1", 250000, "31/31/31/31/31/31");
        upsertMethod.invoke(databaseManager, listing);

        assertEquals(1, countRows("listings"), "First upsert should insert one listing row");
        assertTrue(countRows("pokemon_listings") >= 1, "Pokemon subtype row should be inserted");

        Map<String, Object> updated = pokemonListing("fp-1", 275000, "31/31/31/31/31/30");
        upsertMethod.invoke(databaseManager, updated);

        assertEquals(1, countRows("listings"), "Upsert with same fingerprint should update, not duplicate");
        assertTrue(countRows("pokemon_listings") >= 1, "Pokemon subtype row should still exist after update");
    }

    @Test
    void duplicateMessageReplayDedupesByDeterministicId() throws Exception {
        Map<String, Object> listing = itemListing("replay-fp-42", "Master Ball", 90000, 3);

        upsertMethod.invoke(databaseManager, listing);
        upsertMethod.invoke(databaseManager, listing);
        upsertMethod.invoke(databaseManager, new LinkedHashMap<>(listing));

        assertEquals(1, countRows("listings"), "Replayed duplicate payloads should dedupe into one listing row");
        assertTrue(countRows("item_listings") >= 1, "Item subtype row should remain persisted once");
    }

    private int countRows(String table) throws Exception {
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private Map<String, Object> pokemonListing(String fingerprint, int price, String iv) {
        Map<String, Object> listing = new LinkedHashMap<>();
        listing.put("fingerprint", fingerprint);
        listing.put("listingType", "pokemon");
        listing.put("species", "Gengar");
        listing.put("level", 55);
        listing.put("price", price);
        listing.put("seller", "Ash");
        listing.put("iv", iv);
        return listing;
    }

    private Map<String, Object> itemListing(String fingerprint, String itemName, int price, int qty) {
        Map<String, Object> listing = new LinkedHashMap<>();
        listing.put("fingerprint", fingerprint);
        listing.put("listingType", "item");
        listing.put("item", itemName);
        listing.put("price", price);
        listing.put("quantity", qty);
        listing.put("seller", "Brock");
        return listing;
    }
}
