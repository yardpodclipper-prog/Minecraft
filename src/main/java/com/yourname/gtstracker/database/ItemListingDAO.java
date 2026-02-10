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

public class ItemListingDAO extends ListingDAO {

    public ItemListingDAO(DataSource dataSource) {
        super(dataSource);
    }

    public List<PriceSample> getItemPriceSamples(String itemName, Instant cutoff) throws SQLException {
        String sql = """
            SELECT price, created_at
            FROM listings
            WHERE listing_type = 'ITEM'
              AND status = 'ACTIVE'
              AND item_name = ?
              AND created_at >= ?
            ORDER BY created_at DESC
            """;

        List<PriceSample> samples = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, itemName);
            ps.setTimestamp(2, Timestamp.from(cutoff));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    samples.add(new PriceSample(
                        rs.getBigDecimal("price"),
                        toInstant(rs.getTimestamp("created_at"))
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
