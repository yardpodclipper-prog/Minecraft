package com.yourname.gtstracker.ingest;

import com.yourname.gtstracker.GTSTrackerMod;
import com.yourname.gtstracker.chat.GTSMessageParser;
import com.yourname.gtstracker.database.DatabaseManager;
import com.yourname.gtstracker.database.models.DataSource;
import com.yourname.gtstracker.database.models.ListingData;
import com.yourname.gtstracker.database.models.ListingStatus;
import com.yourname.gtstracker.util.ListingFingerprint;

import java.time.Instant;
import java.util.Optional;

public class ListingIngestionService {
    private final DatabaseManager databaseManager;

    public ListingIngestionService(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Optional<ListingData> ingestChatMessage(String message) {
        if (!GTSMessageParser.isGTSMessage(message)) {
            return Optional.empty();
        }

        ListingData listing;
        try {
            listing = GTSMessageParser.parse(message);
        } catch (RuntimeException ex) {
            GTSTrackerMod.LOGGER.warn("Failed to parse GTS chat message: '{}'", snippet(message), ex);
            return Optional.empty();
        }

        if (listing == null) {
            return Optional.empty();
        }

        Instant seenAt = Instant.now();
        listing.setId(ListingFingerprint.build(listing, seenAt));
        listing.setStatus(ListingStatus.ACTIVE);
        listing.setSourceFirst(DataSource.CHAT);
        listing.setSourceLast(DataSource.CHAT);
        listing.setFirstSeen(seenAt.toEpochMilli());
        listing.setLastSeen(seenAt.toEpochMilli());

        databaseManager.upsertListing(listing);
        GTSTrackerMod.LOGGER.debug("Ingested listing {} from chat", listing.getId());
        return Optional.of(listing);
    }

    private static String snippet(String message) {
        if (message == null) {
            return "";
        }
        String compact = message.replaceAll("\\s+", " ").trim();
        if (compact.length() <= 120) {
            return compact;
        }
        return compact.substring(0, 117) + "...";
    }
}
