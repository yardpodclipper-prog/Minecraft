package com.yourname.gtstracker.util;

import com.yourname.gtstracker.database.models.ItemListing;
import com.yourname.gtstracker.database.models.ListingData;
import com.yourname.gtstracker.database.models.PokemonListing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

public final class ListingFingerprint {
    private ListingFingerprint() {
    }

    public static String build(ListingData listing, Instant seenAt) {
        long minuteBucket = seenAt.getEpochSecond() / 60;
        String base = switch (listing) {
            case PokemonListing pokemon -> String.join("|",
                "POKEMON",
                clean(listing.getSeller()),
                clean(pokemon.getSpecies()),
                String.valueOf(pokemon.isShiny()),
                String.valueOf(pokemon.getLevel()),
                String.valueOf(listing.getPrice()),
                String.valueOf(minuteBucket)
            );
            case ItemListing item -> String.join("|",
                "ITEM",
                clean(listing.getSeller()),
                clean(item.getItemName()),
                String.valueOf(item.getQuantity()),
                String.valueOf(listing.getPrice()),
                String.valueOf(minuteBucket)
            );
            default -> String.join("|",
                clean(listing.getType().name()),
                clean(listing.getSeller()),
                String.valueOf(listing.getPrice()),
                String.valueOf(minuteBucket)
            );
        };

        return sha256(base);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
