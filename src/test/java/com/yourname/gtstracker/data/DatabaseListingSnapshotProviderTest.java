package com.yourname.gtstracker.data;

import com.yourname.gtstracker.database.ListingDAO;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseListingSnapshotProviderTest {

    @Test
    void mapsPokemonAndItemRowsIntoSnapshotOutput() {
        Instant pokemonSeen = Instant.parse("2025-01-10T10:15:30Z");
        Instant itemSeen = Instant.parse("2025-01-10T10:20:30Z");

        ListingDAO.ListingRow pokemonRow = new ListingDAO.ListingRow();
        pokemonRow.id = "poke-1";
        pokemonRow.listingType = "POKEMON";
        pokemonRow.pokemonSpecies = "Garchomp";
        pokemonRow.price = java.math.BigDecimal.valueOf(125000);
        pokemonRow.status = "active";
        pokemonRow.lastSeen = pokemonSeen;

        ListingDAO.ListingRow itemRow = new ListingDAO.ListingRow();
        itemRow.id = "item-1";
        itemRow.listingType = "ITEM";
        itemRow.itemName = "Master Ball";
        itemRow.price = java.math.BigDecimal.valueOf(15000);
        itemRow.status = "sold";
        itemRow.lastSeen = itemSeen;

        DatabaseListingSnapshotProvider provider = new DatabaseListingSnapshotProvider(
            new StubListingDao(List.of(pokemonRow, itemRow), 8, Optional.of(itemSeen)),
            100
        );

        ListingSnapshot snapshot = provider.fetchSnapshot();

        assertEquals(8, snapshot.totalActiveListings());
        assertEquals(itemSeen, snapshot.lastIngestTime());
        assertEquals(2, snapshot.listings().size());

        MarketListing first = snapshot.listings().get(0);
        assertEquals("Garchomp", first.displayName());
        assertEquals(125000d, first.price());
        assertEquals(MarketListing.Status.ACTIVE, first.status());

        MarketListing second = snapshot.listings().get(1);
        assertEquals("Master Ball", second.displayName());
        assertEquals(15000d, second.price());
        assertEquals(MarketListing.Status.SOLD, second.status());
    }

    @Test
    void usesEpochWhenNoRowsHaveLastSeen() {
        DatabaseListingSnapshotProvider provider = new DatabaseListingSnapshotProvider(
            new StubListingDao(List.of(), 0, Optional.empty()),
            100
        );

        ListingSnapshot snapshot = provider.fetchSnapshot();

        assertEquals(Instant.EPOCH, snapshot.lastIngestTime());
    }

    private static final class StubListingDao extends ListingDAO {
        private final List<ListingRow> rows;
        private final int totalActive;
        private final Optional<Instant> maxLastSeen;

        private StubListingDao(List<ListingRow> rows, int totalActive, Optional<Instant> maxLastSeen) {
            super(new NoopDataSource());
            this.rows = rows;
            this.totalActive = totalActive;
            this.maxLastSeen = maxLastSeen;
        }

        @Override
        public List<ListingRow> getActiveListings(int limit, int offset) {
            return rows;
        }

        @Override
        public int getActiveListingsCount() {
            return totalActive;
        }

        @Override
        public Optional<Instant> getMaxLastSeenForActiveListings() {
            return maxLastSeen;
        }
    }

    private static final class NoopDataSource implements DataSource {
        @Override
        public Connection getConnection() throws SQLException {
            throw new SQLException("Not used");
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            throw new SQLException("Not used");
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new SQLException("Not supported");
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }

        @Override
        public java.io.PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(java.io.PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public java.util.logging.Logger getParentLogger() {
            return java.util.logging.Logger.getGlobal();
        }
    }
}
