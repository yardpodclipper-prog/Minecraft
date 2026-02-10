package com.yourname.gtstracker;

import com.yourname.gtstracker.database.models.PokemonListing;
import com.yourname.gtstracker.util.ListingFingerprint;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ListingFingerprintTest {

    @Test
    void stableIdWithinSameMinuteForSameListing() {
        PokemonListing listing = new PokemonListing();
        listing.setSeller("Ash");
        listing.setSpecies("Gengar");
        listing.setShiny(true);
        listing.setLevel(55);
        listing.setPrice(250000);

        String idA = ListingFingerprint.build(listing, Instant.parse("2024-01-01T00:00:01Z"));
        String idB = ListingFingerprint.build(listing, Instant.parse("2024-01-01T00:00:59Z"));

        assertEquals(idA, idB);
    }

    @Test
    void differentIdWhenCoreAttributesChange() {
        PokemonListing listingA = new PokemonListing();
        listingA.setSeller("Ash");
        listingA.setSpecies("Gengar");
        listingA.setLevel(55);
        listingA.setPrice(250000);

        PokemonListing listingB = new PokemonListing();
        listingB.setSeller("Ash");
        listingB.setSpecies("Gengar");
        listingB.setLevel(55);
        listingB.setPrice(275000);

        Instant seenAt = Instant.parse("2024-01-01T00:00:30Z");
        assertNotEquals(ListingFingerprint.build(listingA, seenAt), ListingFingerprint.build(listingB, seenAt));
    }
}
