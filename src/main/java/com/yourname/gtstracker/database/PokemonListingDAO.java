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

public class PokemonListingDAO extends ListingDAO {

    public PokemonListingDAO(DataSource dataSource) {
        super(dataSource);
    }

    public List<PriceSample> getPokemonPriceSamples(String species, boolean shinyOnly, Integer minIv, Instant cutoff) throws SQLException {
        StringBuilder sql = new StringBuilder("""
            SELECT price, created_at
            FROM listings
            WHERE listing_type = 'POKEMON'
              AND status = 'ACTIVE'
              AND pokemon_species = ?
              AND created_at >= ?
            """);

        if (shinyOnly) {
            sql.append(" AND is_shiny = TRUE");
        }
        if (minIv != null) {
            sql.append(" AND iv_total >= ?");
        }
        sql.append(" ORDER BY created_at DESC");

        List<PriceSample> samples = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int index = 1;
            ps.setString(index++, species);
            ps.setTimestamp(index++, Timestamp.from(cutoff));
            if (minIv != null) {
                ps.setInt(index, minIv);
            }

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
