package com.yourname.gtstracker;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Map;

/**
 * Deterministic fingerprinting utility for listing payloads.
 */
public final class ListingFingerprint {
    private ListingFingerprint() {
    }

    public static String fingerprint(Map<?, ?> listing) {
        if (listing == null || listing.isEmpty()) {
            return fingerprint("");
        }

        StringBuilder canonical = new StringBuilder();
        append(canonical, "listingType", listing.get("listingType"));
        append(canonical, "species", listing.get("species"));
        append(canonical, "item", listing.get("item"));
        append(canonical, "level", listing.get("level"));
        append(canonical, "price", listing.get("price"));
        append(canonical, "seller", listing.get("seller"));
        append(canonical, "iv", listing.get("iv"));

        return fingerprint(canonical.toString());
    }

    public static String fingerprint(String source) {
        String normalized = source == null ? "" : source.trim().toLowerCase(Locale.ROOT);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    private static void append(StringBuilder builder, String key, Object value) {
        String normalized = value == null ? "" : String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        builder.append(key).append('=').append(normalized).append('|');
    }
}
