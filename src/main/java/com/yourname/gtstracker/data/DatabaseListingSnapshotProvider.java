package com.yourname.gtstracker.data;

import com.yourname.gtstracker.GTSTrackerMod;
import com.yourname.gtstracker.database.DatabaseManager;
import com.yourname.gtstracker.database.ListingDAO;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

public final class DatabaseListingSnapshotProvider implements ListingSnapshotProvider {
    private static final int DEFAULT_LIMIT = 100;

    private final ListingDAO listingDAO;
    private final int limit;

    public DatabaseListingSnapshotProvider(DatabaseManager databaseManager) {
        this(new ListingDAO(new DatabaseManagerDataSource(databaseManager)), DEFAULT_LIMIT);
    }

    DatabaseListingSnapshotProvider(ListingDAO listingDAO, int limit) {
        this.listingDAO = listingDAO;
        this.limit = limit;
    }

    @Override
    public ListingSnapshot fetchSnapshot() {
        try {
            List<MarketListing> listings = listingDAO.getActiveListings(limit, 0)
                .stream()
                .map(this::toMarketListing)
                .toList();

            int totalActiveListings = listingDAO.getActiveListingsCount();
            Instant lastIngestTime = listingDAO.getMaxLastSeenForActiveListings().orElse(Instant.EPOCH);

            return new ListingSnapshot(listings, totalActiveListings, lastIngestTime);
        } catch (SQLException exception) {
            GTSTrackerMod.LOGGER.error("Failed to build listing snapshot from persistence.", exception);
            return ListingSnapshot.empty();
        }
    }

    private MarketListing toMarketListing(ListingDAO.ListingRow row) {
        String displayName = row.listingType != null && row.listingType.equalsIgnoreCase("POKEMON")
            ? row.pokemonSpecies
            : row.itemName;

        if (displayName == null || displayName.isBlank()) {
            displayName = row.id == null || row.id.isBlank() ? "Unknown Listing" : row.id;
        }

        BigDecimal rowPrice = row.price == null ? BigDecimal.ZERO : row.price;
        Instant lastSeen = row.lastSeen == null ? Instant.EPOCH : row.lastSeen;

        return new MarketListing(
            displayName,
            rowPrice.doubleValue(),
            lastSeen,
            toStatus(row.status)
        );
    }

    private MarketListing.Status toStatus(String status) {
        if (status == null) {
            return MarketListing.Status.EXPIRED;
        }

        return switch (status.toLowerCase()) {
            case "active" -> MarketListing.Status.ACTIVE;
            case "sold" -> MarketListing.Status.SOLD;
            default -> MarketListing.Status.EXPIRED;
        };
    }

    private static final class DatabaseManagerDataSource implements DataSource {
        private final DatabaseManager databaseManager;

        private DatabaseManagerDataSource(DatabaseManager databaseManager) {
            this.databaseManager = databaseManager;
        }

        @Override
        public Connection getConnection() throws SQLException {
            Connection connection = databaseManager.getConnection();
            if (connection == null) {
                throw new SQLException("Database connection is not initialized.");
            }
            return connection;
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return getConnection();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new SQLException("unwrap is not supported");
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
