package com.yourname.gtstracker.database;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
            "CREATE INDEX IF NOT EXISTS idx_listings_status_last_seen ON listings(status, last_seen DESC)",
            "CREATE INDEX IF NOT EXISTS idx_listings_type_status_last_seen ON listings(listing_type, status, last_seen DESC)",
            "CREATE INDEX IF NOT EXISTS idx_pokemon_species_shiny_iv ON pokemon_listings(species, is_shiny, iv_total)",
            "CREATE INDEX IF NOT EXISTS idx_item_name ON item_listings(item_name)"
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
            SELECT l.id,
                   l.listing_type,
                   p.species AS pokemon_species,
                   i.item_name,
                   COALESCE(p.is_shiny, 0) AS is_shiny,
                   p.iv_total,
                   l.price,
                   l.status,
                   l.first_seen,
                   l.last_seen
            FROM listings l
            LEFT JOIN pokemon_listings p ON p.listing_id = l.id
            LEFT JOIN item_listings i ON i.listing_id = l.id
            WHERE l.status = 'active'
            ORDER BY l.last_seen DESC
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
            SELECT l.id,
                   l.listing_type,
                   p.species AS pokemon_species,
                   i.item_name,
                   COALESCE(p.is_shiny, 0) AS is_shiny,
                   p.iv_total,
                   l.price,
                   l.status,
                   l.first_seen,
                   l.last_seen
            FROM listings l
            LEFT JOIN pokemon_listings p ON p.listing_id = l.id
            LEFT JOIN item_listings i ON i.listing_id = l.id
            WHERE l.last_seen >= ?
            ORDER BY l.last_seen DESC
            """;

        List<ListingRow> rows = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, timestamp.toEpochMilli());
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
                            WHEN status = 'active' AND last_seen < ? THEN 'expired'
                            WHEN status = 'active' AND last_seen < ? THEN 'unknown'
                            ELSE status
                         END
            WHERE status = 'active'
              AND last_seen < ?
            """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            long expiredMillis = expiredBefore.toEpochMilli();
            long missingMillis = missingSeenBefore.toEpochMilli();
            long earliest = Math.min(expiredMillis, missingMillis);
            ps.setLong(1, expiredMillis);
            ps.setLong(2, missingMillis);
            ps.setLong(3, earliest);
            return ps.executeUpdate();
        }
    }

    /**
     * Optional helper to validate index usage with EXPLAIN.
     */
    public List<String> explainQueryPlan(String sql) throws SQLException {
        List<String> plan = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("EXPLAIN QUERY PLAN " + sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                plan.add(rs.getString("detail"));
            }
        }
        return plan;
    }

    protected ListingRow mapListingRow(ResultSet rs) throws SQLException {
        ListingRow row = new ListingRow();
        row.id = rs.getString("id");
        row.listingType = rs.getString("listing_type");
        row.pokemonSpecies = rs.getString("pokemon_species");
        row.itemName = rs.getString("item_name");
        row.shiny = rs.getBoolean("is_shiny");
        row.ivTotal = (Integer) rs.getObject("iv_total");
        row.price = rs.getBigDecimal("price");
        row.status = rs.getString("status");
        row.firstSeen = toInstant(rs.getLong("first_seen"));
        row.lastSeen = toInstant(rs.getLong("last_seen"));
        return row;
    }

    protected Instant toInstant(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis);
    }

    public static class ListingRow {
        public String id;
        public String listingType;
        public String pokemonSpecies;
        public String itemName;
        public boolean shiny;
        public Integer ivTotal;
        public BigDecimal price;
        public String status;
        public Instant firstSeen;
        public Instant lastSeen;
    }
}
