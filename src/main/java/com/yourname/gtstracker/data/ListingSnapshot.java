package com.yourname.gtstracker.data;

import java.time.Instant;
import java.util.List;

public record ListingSnapshot(List<MarketListing> listings, int totalActiveListings, Instant lastIngestTime) {
    public static ListingSnapshot empty() {
        return new ListingSnapshot(List.of(), 0, Instant.EPOCH);
    }
}
