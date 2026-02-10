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

public class PokemonListingDAO extends ListingDAO {

    public PokemonListingDAO(DataSource dataSource) {
        super(dataSource);
    }

    public List<PriceSample> getPokemonPriceSamples(String species, boolean shinyOnly, Integer minIv, Instant cutoff) throws SQLException {
        StringBuilder sql = new StringBuilder("""
            SELECT l.price, l.last_seen
            FROM listings l
            JOIN pokemon_listings p ON p.listing_id = l.id
            WHERE l.listing_type = 'POKEMON'
              AND l.status = 'active'
              AND p.species = ?
              AND l.last_seen >= ?
            """);

        if (shinyOnly) {
            sql.append(" AND p.is_shiny = 1");
        }
        if (minIv != null) {
            sql.append(" AND p.iv_total >= ?");
        }
        sql.append(" ORDER BY l.last_seen DESC");

        List<PriceSample> samples = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int index = 1;
            ps.setString(index++, species);
            ps.setLong(index++, cutoff.toEpochMilli());
            if (minIv != null) {
                ps.setInt(index, minIv);
            }

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
