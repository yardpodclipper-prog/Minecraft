package com.yourname.gtstracker.data;

import java.time.Instant;

public record MarketListing(String displayName, double price, Instant lastSeen, Status status) {
    public enum Status {
        ACTIVE,
        SOLD,
        EXPIRED
    }
}
