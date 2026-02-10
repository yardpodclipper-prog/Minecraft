package com.yourname.gtstracker.database;

import com.yourname.gtstracker.GTSTrackerMod;
import com.yourname.gtstracker.database.models.DataSource;
import com.yourname.gtstracker.database.models.ItemListing;
import com.yourname.gtstracker.database.models.ListingData;
import com.yourname.gtstracker.database.models.PokemonListing;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class DatabaseManager {
    private static final String DB_FILE = "gtstracker.db";

    private static final List<MigrationStep> MIGRATIONS = List.of(
        new MigrationStep(1, "baseline_schema", "db/migration/V1__baseline_schema.sql"),
        new MigrationStep(2, "listing_indexes", "db/migration/V2__listing_indexes.sql"),
        new MigrationStep(3, "align_listing_dao_schema", "db/migration/V3__align_listing_dao_schema.sql")
    );

    private final String jdbcUrlOverride;
    private Connection connection;
    private SchemaStatus schemaStatus = SchemaStatus.uninitialized();

    public DatabaseManager() {
        this(null);
    }

    public DatabaseManager(String jdbcUrlOverride) {
        this.jdbcUrlOverride = jdbcUrlOverride;
    }

    public void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            String jdbcUrl = resolveJdbcUrl();

            connection = DriverManager.getConnection(jdbcUrl);
            connection.setAutoCommit(true);

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
                stmt.execute("PRAGMA journal_mode = WAL");
            }

            applyMigrations();
            verifySchemaIntegrity();
            GTSTrackerMod.LOGGER.info("Database initialized at {} ({})", jdbcUrl, schemaStatus.toStatusLine());
        } catch (Exception e) {
            schemaStatus = SchemaStatus.failed("initialization error", -1, MIGRATIONS.size(), List.of(e.getMessage()));
            GTSTrackerMod.LOGGER.error("Failed to initialize SQLite database.", e);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public String getMigrationStatusSummary() {
        return schemaStatus.toStatusLine();
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

    public boolean upsertListing(ListingData listing) {
        if (connection == null) {
            return false;
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
            return true;
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackError) {
                GTSTrackerMod.LOGGER.error("Failed to rollback listing upsert.", rollbackError);
            }
            GTSTrackerMod.LOGGER.error("Failed to upsert listing {}", listing.getId(), e);
            return false;
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

    private String resolveJdbcUrl() throws Exception {
        if (jdbcUrlOverride != null && !jdbcUrlOverride.isBlank()) {
            return jdbcUrlOverride;
        }

        Path dbDir = FabricLoader.getInstance().getGameDir().resolve("config").resolve("gtstracker");
        Files.createDirectories(dbDir);
        Path dbPath = dbDir.resolve(DB_FILE);
        return "jdbc:sqlite:" + dbPath.toAbsolutePath();
    }

    private void applyMigrations() throws SQLException {
        ensureMigrationTable();
        int currentVersion = getCurrentSchemaVersion();
        List<MigrationStep> pending = MIGRATIONS.stream()
            .filter(step -> step.version() > currentVersion)
            .sorted(Comparator.comparingInt(MigrationStep::version))
            .toList();

        for (MigrationStep step : pending) {
            GTSTrackerMod.LOGGER.info("Applying DB migration V{} ({})", step.version(), step.name());
            runSqlScript(step.resourcePath());
            recordMigration(step);
        }

        int finalVersion = getCurrentSchemaVersion();
        schemaStatus = SchemaStatus.ready(finalVersion, MIGRATIONS.size(), pending.size(), List.of());
    }

    private void verifySchemaIntegrity() {
        if (connection == null) {
            return;
        }

        List<String> issues = new ArrayList<>();
        try {
            verifyColumns(issues, "listings", "id", "listing_type", "seller", "price", "first_seen", "last_seen", "status", "source_first", "source_last");
            verifyColumns(issues, "pokemon_listings", "listing_id", "species", "is_shiny", "iv_total");
            verifyColumns(issues, "item_listings", "listing_id", "item_name", "quantity");
            verifyIndexes(issues, "idx_listings_status_last_seen", "idx_listings_type_status_last_seen", "idx_pokemon_species_shiny_iv", "idx_item_name");
        } catch (SQLException e) {
            issues.add("Schema verification failed unexpectedly: " + e.getMessage());
        }

        if (!issues.isEmpty()) {
            issues.forEach(issue -> GTSTrackerMod.LOGGER.error(
                "Database schema mismatch: {}. Action: back up db and restart to rerun migrations, or manually repair schema.",
                issue
            ));
            schemaStatus = SchemaStatus.failed("schema verification failed", getSafeVersion(), MIGRATIONS.size(), issues);
        }
    }

    private int getSafeVersion() {
        try {
            return getCurrentSchemaVersion();
        } catch (SQLException ignored) {
            return -1;
        }
    }

    private void verifyColumns(List<String> issues, String tableName, String... requiredColumns) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        List<String> present = new ArrayList<>();
        try (ResultSet rs = metaData.getColumns(null, null, tableName, null)) {
            while (rs.next()) {
                present.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
            }
        }

        for (String requiredColumn : requiredColumns) {
            if (!present.contains(requiredColumn.toLowerCase(Locale.ROOT))) {
                issues.add("missing column " + tableName + "." + requiredColumn);
            }
        }
    }

    private void verifyIndexes(List<String> issues, String... requiredIndexes) throws SQLException {
        List<String> present = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT name FROM sqlite_master WHERE type = 'index'");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                present.add(rs.getString("name"));
            }
        }

        for (String requiredIndex : requiredIndexes) {
            if (!present.contains(requiredIndex)) {
                issues.add("missing index " + requiredIndex);
            }
        }
    }

    private void ensureMigrationTable() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS schema_version (
                    version INTEGER PRIMARY KEY,
                    name TEXT NOT NULL,
                    applied_at INTEGER NOT NULL
                )
                """);
        }
    }

    private int getCurrentSchemaVersion() throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT COALESCE(MAX(version), 0) AS v FROM schema_version");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt("v") : 0;
        }
    }

    private void runSqlScript(String resourcePath) throws SQLException {
        String sql = readResource(resourcePath);
        try (Statement stmt = connection.createStatement()) {
            for (String statement : sql.split(";")) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
        }
    }

    private void recordMigration(MigrationStep step) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
            "INSERT INTO schema_version(version, name, applied_at) VALUES (?, ?, ?)")) {
            ps.setInt(1, step.version());
            ps.setString(2, step.name());
            ps.setLong(3, Instant.now().toEpochMilli());
            ps.executeUpdate();
        }
    }

    private String readResource(String resourcePath) {
        InputStream stream = DatabaseManager.class.getClassLoader().getResourceAsStream(resourcePath);
        if (stream == null) {
            throw new IllegalStateException("Missing migration resource: " + resourcePath);
        }

        return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));
    }

    private record MigrationStep(int version, String name, String resourcePath) {
    }

    private record SchemaStatus(boolean ready, int version, int expectedVersion, int appliedThisStartup, String message,
                                List<String> issues) {
        static SchemaStatus uninitialized() {
            return new SchemaStatus(false, 0, MIGRATIONS.size(), 0, "not initialized", List.of());
        }

        static SchemaStatus ready(int version, int expected, int appliedThisStartup, List<String> issues) {
            return new SchemaStatus(true, version, expected, appliedThisStartup, "ready", issues);
        }

        static SchemaStatus failed(String message, int version, int expected, List<String> issues) {
            return new SchemaStatus(false, version, expected, 0, message, issues);
        }

        String toStatusLine() {
            String base = String.format(
                Locale.ROOT,
                "schema v%d/%d (%s, migrations-applied-this-startup=%d)",
                version,
                expectedVersion,
                ready ? "ready" : message,
                appliedThisStartup
            );
            if (issues.isEmpty()) {
                return base;
            }
            return base + "; issues=" + String.join(" | ", issues);
        }
    }
}
