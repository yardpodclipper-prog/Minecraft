package com.yourname.gtstracker;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lightweight SQLite manager used by bootstrap/unit tests.
 */
public final class DatabaseManager implements AutoCloseable {
    private final String jdbcUrl;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public DatabaseManager(String jdbcUrl) {
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl, "jdbcUrl");
        try {
            migrateSchema();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to migrate schema", e);
        }
    }

    public void initialize() {
        initialized.set(true);
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public boolean upsert(Map<String, Object> listing) {
        if (listing == null || listing.isEmpty()) {
            return false;
        }

        String fingerprint = asString(listing.get("fingerprint"));
        if (fingerprint.isEmpty()) {
            fingerprint = ListingFingerprint.fingerprint(listing);
        }

        String listingType = asString(listing.get("listingType")).toLowerCase(Locale.ROOT);
        if (listingType.isEmpty()) {
            listingType = listing.containsKey("species") ? "pokemon" : "item";
        }

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                long listingId = upsertBaseListing(connection, fingerprint, listingType, listing);
                if ("pokemon".equals(listingType)) {
                    upsertPokemon(connection, listingId, listing);
                    deleteSubtype(connection, "item_listings", listingId);
                } else {
                    upsertItem(connection, listingId, listing);
                    deleteSubtype(connection, "pokemon_listings", listingId);
                }
                connection.commit();
                return true;
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to upsert listing", e);
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    private void migrateSchema() throws SQLException {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS listings (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    fingerprint TEXT NOT NULL UNIQUE,
                    listing_type TEXT NOT NULL,
                    seller TEXT,
                    price INTEGER,
                    updated_at INTEGER
                )
            """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS pokemon_listings (
                    listing_id INTEGER PRIMARY KEY,
                    species TEXT,
                    level INTEGER,
                    iv TEXT,
                    FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE
                )
            """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS item_listings (
                    listing_id INTEGER PRIMARY KEY,
                    item_name TEXT,
                    quantity INTEGER,
                    FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE
                )
            """);
        }
    }

    private long upsertBaseListing(
        Connection connection,
        String fingerprint,
        String listingType,
        Map<String, Object> listing
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO listings (fingerprint, listing_type, seller, price, updated_at)
            VALUES (?, ?, ?, ?, strftime('%s', 'now'))
            ON CONFLICT(fingerprint) DO UPDATE SET
                listing_type = excluded.listing_type,
                seller = excluded.seller,
                price = excluded.price,
                updated_at = excluded.updated_at
        """)) {
            statement.setString(1, fingerprint);
            statement.setString(2, listingType);
            statement.setString(3, asString(listing.get("seller")));
            statement.setObject(4, asInteger(listing.get("price")));
            statement.executeUpdate();
        }

        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT id FROM listings WHERE fingerprint = ?"
        )) {
            statement.setString(1, fingerprint);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
            }
        }

        throw new SQLException("Failed to resolve listing id for fingerprint " + fingerprint);
    }

    private void upsertPokemon(Connection connection, long listingId, Map<String, Object> listing) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO pokemon_listings (listing_id, species, level, iv)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(listing_id) DO UPDATE SET
                species = excluded.species,
                level = excluded.level,
                iv = excluded.iv
        """)) {
            statement.setLong(1, listingId);
            statement.setString(2, asString(listing.get("species")));
            statement.setObject(3, asInteger(listing.get("level")));
            statement.setString(4, asString(listing.get("iv")));
            statement.executeUpdate();
        }
    }

    private void upsertItem(Connection connection, long listingId, Map<String, Object> listing) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO item_listings (listing_id, item_name, quantity)
            VALUES (?, ?, ?)
            ON CONFLICT(listing_id) DO UPDATE SET
                item_name = excluded.item_name,
                quantity = excluded.quantity
        """)) {
            statement.setLong(1, listingId);
            statement.setString(2, asString(listing.get("item")));
            statement.setObject(3, asInteger(listing.get("quantity")));
            statement.executeUpdate();
        }
    }

    private static void deleteSubtype(Connection connection, String table, long listingId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM " + table + " WHERE listing_id = ?")) {
            statement.setLong(1, listingId);
            statement.executeUpdate();
        }
    }

    private static String asString(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = String.valueOf(value).replaceAll("[^0-9-]", "").trim();
        if (text.isEmpty() || "-".equals(text)) {
            return null;
        }
        return Integer.parseInt(text);
    }

    @Override
    public void close() {
        initialized.set(false);
    }
}
