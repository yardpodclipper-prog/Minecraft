package com.yourname.gtstracker.database;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Base DAO with shared listing queries and lifecycle hooks.
 */
public class ListingDAO {

    protected final DataSource dataSource;

    public ListingDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Creates supporting indexes used by the read-heavy listing queries.
     */
    public void ensureIndexes() throws SQLException {
        String[] statements = {
            // Covers active-listing pagination by status + recency.
            "CREATE INDEX IF NOT EXISTS idx_listings_status_created_at ON listings(status, created_at DESC)",
            // Covers recent-listing scan by updated_at cutoff.
            "CREATE INDEX IF NOT EXISTS idx_listings_updated_at ON listings(updated_at DESC)",
            // Covers pokemon sample lookup filters and sort.
            "CREATE INDEX IF NOT EXISTS idx_listings_pokemon_samples ON listings(listing_type, pokemon_species, is_shiny, iv_total, created_at DESC)",
            // Covers item sample lookup filters and sort.
            "CREATE INDEX IF NOT EXISTS idx_listings_item_samples ON listings(listing_type, item_name, created_at DESC)",
            // Supports future lifecycle updates (missing/expired transitions).
            "CREATE INDEX IF NOT EXISTS idx_listings_lifecycle ON listings(status, last_seen_at, expires_at)"
        };

        try (Connection connection = dataSource.getConnection()) {
            for (String statement : statements) {
                try (PreparedStatement ps = connection.prepareStatement(statement)) {
                    ps.execute();
                }
            }
        }
    }

    public List<ListingRow> getActiveListings(int limit, int offset) throws SQLException {
        String sql = """
            SELECT id,
                   listing_type,
                   pokemon_species,
                   item_name,
                   is_shiny,
                   iv_total,
                   price,
                   status,
                   created_at,
                   updated_at,
                   last_seen_at,
                   expires_at
            FROM listings
            WHERE status = 'ACTIVE'
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
            """;

        List<ListingRow> rows = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapListingRow(rs));
                }
            }
        }
        return rows;
    }

    public List<ListingRow> getRecentListingsSince(Instant timestamp) throws SQLException {
        String sql = """
            SELECT id,
                   listing_type,
                   pokemon_species,
                   item_name,
                   is_shiny,
                   iv_total,
                   price,
                   status,
                   created_at,
                   updated_at,
                   last_seen_at,
                   expires_at
            FROM listings
            WHERE updated_at >= ?
            ORDER BY updated_at DESC
            """;

        List<ListingRow> rows = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(timestamp));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapListingRow(rs));
                }
            }
        }
        return rows;
    }

    /**
     * Hook for future status lifecycle logic.
     *
     * @return number of rows marked as missing or expired
     */
    public int markMissingOrExpired(Instant missingSeenBefore, Instant expiredBefore) throws SQLException {
        String sql = """
            UPDATE listings
            SET status = CASE
                            WHEN status = 'ACTIVE' AND expires_at IS NOT NULL AND expires_at < ? THEN 'EXPIRED'
                            WHEN status = 'ACTIVE' AND last_seen_at IS NOT NULL AND last_seen_at < ? THEN 'MISSING'
                            ELSE status
                         END,
                updated_at = CURRENT_TIMESTAMP
            WHERE status = 'ACTIVE'
              AND (
                    (expires_at IS NOT NULL AND expires_at < ?)
                 OR (last_seen_at IS NOT NULL AND last_seen_at < ?)
              )
            """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            Timestamp expiredTimestamp = Timestamp.from(expiredBefore);
            Timestamp missingTimestamp = Timestamp.from(missingSeenBefore);
            ps.setTimestamp(1, expiredTimestamp);
            ps.setTimestamp(2, missingTimestamp);
            ps.setTimestamp(3, expiredTimestamp);
            ps.setTimestamp(4, missingTimestamp);
            return ps.executeUpdate();
        }
    }

    /**
     * Optional helper to validate index usage with EXPLAIN.
     */
    public List<String> explainQueryPlan(String sql) throws SQLException {
        List<String> plan = new ArrayList<>();
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement("EXPLAIN " + sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    plan.add(rs.getString(1));
                }
                return plan;
            } catch (SQLException ignored) {
                // SQLite fallback.
                try (PreparedStatement ps = connection.prepareStatement("EXPLAIN QUERY PLAN " + sql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        plan.add(rs.getString("detail"));
                    }
                }
            }
        }
        return plan;
    }

    protected ListingRow mapListingRow(ResultSet rs) throws SQLException {
        ListingRow row = new ListingRow();
        row.id = rs.getLong("id");
        row.listingType = rs.getString("listing_type");
        row.pokemonSpecies = rs.getString("pokemon_species");
        row.itemName = rs.getString("item_name");
        row.shiny = rs.getBoolean("is_shiny");
        row.ivTotal = rs.getInt("iv_total");
        row.price = rs.getBigDecimal("price");
        row.status = rs.getString("status");
        row.createdAt = toInstant(rs.getTimestamp("created_at"));
        row.updatedAt = toInstant(rs.getTimestamp("updated_at"));
        row.lastSeenAt = toInstant(rs.getTimestamp("last_seen_at"));
        row.expiresAt = toInstant(rs.getTimestamp("expires_at"));
        return row;
    }

    protected Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    public static class ListingRow {
        public long id;
        public String listingType;
        public String pokemonSpecies;
        public String itemName;
        public boolean shiny;
        public int ivTotal;
        public BigDecimal price;
        public String status;
        public Instant createdAt;
        public Instant updatedAt;
        public Instant lastSeenAt;
        public Instant expiresAt;
    }
}
