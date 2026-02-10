import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Simple SQLite-backed database manager with startup schema migration and idempotent upsert behavior.
 */
public final class DatabaseManager {
    private final String jdbcUrl;

    public DatabaseManager(String jdbcUrl) throws SQLException {
        this.jdbcUrl = jdbcUrl;
        migrateSchema();
    }

    private Connection openConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (PreparedStatement pragma = connection.prepareStatement("PRAGMA foreign_keys = ON")) {
            pragma.execute();
        }
        return connection;
    }

    /**
     * Creates and migrates schema on startup.
     */
    public void migrateSchema() throws SQLException {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement createListings = connection.prepareStatement(
                    """
                    CREATE TABLE IF NOT EXISTS listings (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        fingerprint TEXT NOT NULL UNIQUE,
                        source_message_id TEXT,
                        seller TEXT,
                        price INTEGER,
                        created_at TEXT,
                        updated_at TEXT
                    )
                    """)) {
                    createListings.execute();
                }

                try (PreparedStatement createPokemonListings = connection.prepareStatement(
                    """
                    CREATE TABLE IF NOT EXISTS pokemon_listings (
                        listing_id INTEGER PRIMARY KEY,
                        species TEXT,
                        level INTEGER,
                        nature TEXT,
                        shiny INTEGER NOT NULL DEFAULT 0,
                        updated_at TEXT,
                        FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE
                    )
                    """)) {
                    createPokemonListings.execute();
                }

                try (PreparedStatement createItemListings = connection.prepareStatement(
                    """
                    CREATE TABLE IF NOT EXISTS item_listings (
                        listing_id INTEGER PRIMARY KEY,
                        item_name TEXT,
                        quantity INTEGER,
                        metadata TEXT,
                        updated_at TEXT,
                        FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE
                    )
                    """)) {
                    createItemListings.execute();
                }

                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    /**
     * Upsert a listing and return its canonical listing id.
     */
    public long upsertListing(
        String fingerprint,
        String sourceMessageId,
        String seller,
        Integer price,
        String createdAt,
        String updatedAt
    ) throws SQLException {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO listings (fingerprint, source_message_id, seller, price, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT(fingerprint) DO UPDATE SET
                        source_message_id = excluded.source_message_id,
                        seller = excluded.seller,
                        price = excluded.price,
                        created_at = COALESCE(listings.created_at, excluded.created_at),
                        updated_at = excluded.updated_at
                    """)) {
                    statement.setString(1, fingerprint);
                    statement.setString(2, sourceMessageId);
                    statement.setString(3, seller);
                    if (price == null) {
                        statement.setObject(4, null);
                    } else {
                        statement.setInt(4, price);
                    }
                    statement.setString(5, createdAt);
                    statement.setString(6, updatedAt);
                    statement.executeUpdate();
                }

                long listingId;
                try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id FROM listings WHERE fingerprint = ?")) {
                    statement.setString(1, fingerprint);
                    try (ResultSet rs = statement.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Failed to resolve listing id for fingerprint: " + fingerprint);
                        }
                        listingId = rs.getLong("id");
                    }
                }

                connection.commit();
                return listingId;
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    /**
     * Upsert pokemon subtype row linked to listing.
     */
    public void upsertPokemonListing(
        long listingId,
        String species,
        Integer level,
        String nature,
        boolean shiny,
        String updatedAt
    ) throws SQLException {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO pokemon_listings (listing_id, species, level, nature, shiny, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT(listing_id) DO UPDATE SET
                        species = excluded.species,
                        level = excluded.level,
                        nature = excluded.nature,
                        shiny = excluded.shiny,
                        updated_at = excluded.updated_at
                    """)) {
                    statement.setLong(1, listingId);
                    statement.setString(2, species);
                    if (level == null) {
                        statement.setObject(3, null);
                    } else {
                        statement.setInt(3, level);
                    }
                    statement.setString(4, nature);
                    statement.setInt(5, shiny ? 1 : 0);
                    statement.setString(6, updatedAt);
                    statement.executeUpdate();
                }

                // Ensure single active subtype row by removing opposite subtype when type changes.
                try (PreparedStatement cleanup = connection.prepareStatement(
                    "DELETE FROM item_listings WHERE listing_id = ?")) {
                    cleanup.setLong(1, listingId);
                    cleanup.executeUpdate();
                }

                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    /**
     * Upsert item subtype row linked to listing.
     */
    public void upsertItemListing(
        long listingId,
        String itemName,
        Integer quantity,
        String metadata,
        String updatedAt
    ) throws SQLException {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO item_listings (listing_id, item_name, quantity, metadata, updated_at)
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT(listing_id) DO UPDATE SET
                        item_name = excluded.item_name,
                        quantity = excluded.quantity,
                        metadata = excluded.metadata,
                        updated_at = excluded.updated_at
                    """)) {
                    statement.setLong(1, listingId);
                    statement.setString(2, itemName);
                    if (quantity == null) {
                        statement.setObject(3, null);
                    } else {
                        statement.setInt(3, quantity);
                    }
                    statement.setString(4, metadata);
                    statement.setString(5, updatedAt);
                    statement.executeUpdate();
                }

                // Ensure single active subtype row by removing opposite subtype when type changes.
                try (PreparedStatement cleanup = connection.prepareStatement(
                    "DELETE FROM pokemon_listings WHERE listing_id = ?")) {
                    cleanup.setLong(1, listingId);
                    cleanup.executeUpdate();
                }

                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }
}
