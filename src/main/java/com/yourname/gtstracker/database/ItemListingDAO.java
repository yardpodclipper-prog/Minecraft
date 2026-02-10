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

public class ItemListingDAO extends ListingDAO {

    public ItemListingDAO(DataSource dataSource) {
        super(dataSource);
    }

    public List<PriceSample> getItemPriceSamples(String itemName, Instant cutoff) throws SQLException {
        String sql = """
            SELECT l.price, l.last_seen
            FROM listings l
            JOIN item_listings i ON i.listing_id = l.id
            WHERE l.listing_type = 'ITEM'
              AND l.status = 'active'
              AND i.item_name = ?
              AND l.last_seen >= ?
            ORDER BY l.last_seen DESC
            """;

        List<PriceSample> samples = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, itemName);
            ps.setLong(2, cutoff.toEpochMilli());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    samples.add(new PriceSample(
                        rs.getBigDecimal("price"),
                        toInstant(rs.getLong("last_seen"))
                    ));
                }
            }
        }
        return samples;
    }

    public static class PriceSample {
        public final BigDecimal price;
        public final Instant observedAt;

        public PriceSample(BigDecimal price, Instant observedAt) {
            this.price = price;
            this.observedAt = observedAt;
        }
    }
}
