package com.yourname.gtstracker.database;

import com.yourname.gtstracker.GTSTrackerMod;
import com.yourname.gtstracker.database.models.DataSource;
import com.yourname.gtstracker.database.models.ItemListing;
import com.yourname.gtstracker.database.models.ListingData;
import com.yourname.gtstracker.database.models.PokemonListing;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

public class DatabaseManager {
    private static final String DB_FILE = "gtstracker.db";

    private Connection connection;

    public void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            Path dbDir = FabricLoader.getInstance().getGameDir().resolve("config").resolve("gtstracker");
            Files.createDirectories(dbDir);
            Path dbPath = dbDir.resolve(DB_FILE);

            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
            connection.setAutoCommit(true);

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
                stmt.execute("PRAGMA journal_mode = WAL");
            }

            createSchema();
            GTSTrackerMod.LOGGER.info("Database initialized at {}", dbPath.toAbsolutePath());
        } catch (Exception e) {
            GTSTrackerMod.LOGGER.error("Failed to initialize SQLite database.", e);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public int getTotalListingsCount() {
        if (connection == null) {
            return 0;
        }
        try (var statement = connection.createStatement();
             var rs = statement.executeQuery("SELECT COUNT(*) AS c FROM listings")) {
            return rs.next() ? rs.getInt("c") : 0;
        } catch (SQLException e) {
            GTSTrackerMod.LOGGER.error("Failed to query listings count.", e);
            return 0;
        }
    }

    public void upsertListing(ListingData listing) {
        if (connection == null) {
            return;
        }

        String baseSql = """
            INSERT INTO listings (id, listing_type, seller, price, first_seen, last_seen, status, source_first, source_last)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                last_seen = excluded.last_seen,
                status = excluded.status,
                source_last = excluded.source_last,
                price = excluded.price
            """;

        try {
            connection.setAutoCommit(false);
            try (PreparedStatement stmt = connection.prepareStatement(baseSql)) {
                stmt.setString(1, listing.getId());
                stmt.setString(2, listing.getType().name());
                stmt.setString(3, listing.getSeller());
                stmt.setInt(4, listing.getPrice());
                stmt.setLong(5, listing.getFirstSeen());
                stmt.setLong(6, listing.getLastSeen());
                stmt.setString(7, listing.getStatus().name().toLowerCase(Locale.ROOT));
                stmt.setString(8, toDbSource(listing.getSourceFirst()));
                stmt.setString(9, toDbSource(listing.getSourceLast()));
                stmt.executeUpdate();
            }

            if (listing instanceof PokemonListing pokemon) {
                upsertPokemon(pokemon);
            } else if (listing instanceof ItemListing item) {
                upsertItem(item);
            }

            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackError) {
                GTSTrackerMod.LOGGER.error("Failed to rollback listing upsert.", rollbackError);
            }
            GTSTrackerMod.LOGGER.error("Failed to upsert listing {}", listing.getId(), e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                GTSTrackerMod.LOGGER.error("Failed to restore auto-commit.", e);
            }
        }
    }

    private void upsertPokemon(PokemonListing listing) throws SQLException {
        String sql = """
            INSERT INTO pokemon_listings (listing_id, species, level, is_shiny, iv_hp, iv_atk, iv_def, iv_spatk, iv_spdef, iv_speed, nature, ability)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(listing_id) DO UPDATE SET
                species = excluded.species,
                level = excluded.level,
                is_shiny = excluded.is_shiny,
                iv_hp = excluded.iv_hp,
                iv_atk = excluded.iv_atk,
                iv_def = excluded.iv_def,
                iv_spatk = excluded.iv_spatk,
                iv_spdef = excluded.iv_spdef,
                iv_speed = excluded.iv_speed,
                nature = excluded.nature,
                ability = excluded.ability
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, listing.getId());
            stmt.setString(2, listing.getSpecies());
            stmt.setInt(3, listing.getLevel());
            stmt.setInt(4, listing.isShiny() ? 1 : 0);
            if (listing.getIvs() != null) {
                stmt.setInt(5, listing.getIvs().getHp());
                stmt.setInt(6, listing.getIvs().getAtk());
                stmt.setInt(7, listing.getIvs().getDef());
                stmt.setInt(8, listing.getIvs().getSpatk());
                stmt.setInt(9, listing.getIvs().getSpdef());
                stmt.setInt(10, listing.getIvs().getSpeed());
            } else {
                stmt.setNull(5, java.sql.Types.INTEGER);
                stmt.setNull(6, java.sql.Types.INTEGER);
                stmt.setNull(7, java.sql.Types.INTEGER);
                stmt.setNull(8, java.sql.Types.INTEGER);
                stmt.setNull(9, java.sql.Types.INTEGER);
                stmt.setNull(10, java.sql.Types.INTEGER);
            }
            stmt.setString(11, listing.getNature());
            stmt.setString(12, listing.getAbility());
            stmt.executeUpdate();
        }
    }

    private void upsertItem(ItemListing listing) throws SQLException {
        String sql = """
            INSERT INTO item_listings (listing_id, item_name, quantity)
            VALUES (?, ?, ?)
            ON CONFLICT(listing_id) DO UPDATE SET
                item_name = excluded.item_name,
                quantity = excluded.quantity
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, listing.getId());
            stmt.setString(2, listing.getItemName());
            stmt.setInt(3, listing.getQuantity());
            stmt.executeUpdate();
        }
    }

    private String toDbSource(DataSource source) {
        return source == null ? "chat" : source.name().toLowerCase(Locale.ROOT);
    }

    private void createSchema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS listings (
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

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS pokemon_listings (
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

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS item_listings (
                    listing_id TEXT PRIMARY KEY,
                    item_name TEXT NOT NULL,
                    quantity INTEGER DEFAULT 1,
                    extra_data TEXT,
                    FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE
                )
            """);

            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_listings_status ON listings(status)
            """);
            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_listings_type ON listings(listing_type)
            """);
            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_listings_last_seen ON listings(last_seen)
            """);
            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_pokemon_species ON pokemon_listings(species)
            """);
            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_item_name ON item_listings(item_name)
            """);
        }
    }
}
