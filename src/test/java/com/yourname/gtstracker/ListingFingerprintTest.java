package com.yourname.gtstracker;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ListingFingerprintTest {

    private static Class<?> fingerprintClass;
    private static Method fingerprintMethod;

    @BeforeAll
    static void setup() throws Exception {
        fingerprintClass = Class.forName("com.yourname.gtstracker.ListingFingerprint");
        fingerprintMethod = Arrays.stream(fingerprintClass.getDeclaredMethods())
                .filter(m -> m.getName().toLowerCase(Locale.ROOT).matches(".*(fingerprint|id|hash).*$"))
                .filter(m -> m.getParameterCount() == 1)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No fingerprint/id/hash single-arg method found"));
        fingerprintMethod.setAccessible(true);
    }

    @Test
    void stableIdsForSemanticallySameNormalizedInput() throws Exception {
        Map<String, Object> listingA = baseListing();
        Map<String, Object> listingB = baseListing();

        listingB.put("species", "   gengar  ");
        listingB.put("seller", "ash");

        String idA = String.valueOf(fingerprintMethod.invoke(null, listingA));
        String idB = String.valueOf(fingerprintMethod.invoke(null, listingB));

        assertEquals(idA, idB, "Fingerprint should remain stable across normalization-only differences");
    }

    @Test
    void differentIdsWhenKeyAttributesChange() throws Exception {
        Map<String, Object> listingA = baseListing();
        Map<String, Object> listingB = baseListing();
        listingB.put("price", 275000);

        String idA = String.valueOf(fingerprintMethod.invoke(null, listingA));
        String idB = String.valueOf(fingerprintMethod.invoke(null, listingB));

        assertNotEquals(idA, idB, "Changing key attributes should produce a distinct fingerprint");
    }

    private Map<String, Object> baseListing() {
        Map<String, Object> listing = new LinkedHashMap<>();
        listing.put("listingType", "pokemon");
        listing.put("species", "Gengar");
        listing.put("level", 55);
        listing.put("price", 250000);
        listing.put("seller", "Ash");
        listing.put("iv", "31/31/31/31/31/31");
        return listing;
    }
}
