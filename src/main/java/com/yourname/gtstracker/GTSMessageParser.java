package com.yourname.gtstracker;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses listing-like messages into a normalized key/value map.
 */
public final class GTSMessageParser {
    private static final Pattern GTS_POKEMON = Pattern.compile(
        "(?i)^\\[GTS\\]\\s*SELL\\s*Pokemon\\s*\\|\\s*Species:\\s*(?<species>[^|]+)\\|\\s*Level:\\s*(?<level>\\d+)\\s*\\|\\s*IV:\\s*(?<iv>[^|]+)\\|\\s*Price:\\s*(?<price>[0-9,]+)\\s*$"
    );

    private static final Pattern GTS_ITEM = Pattern.compile(
        "(?i)^\\[GTS\\]\\s*SELL\\s*Item\\s*\\|\\s*Item:\\s*(?<item>[^|]+)\\|\\s*Qty:\\s*(?<qty>\\d+)\\s*\\|\\s*Price:\\s*(?<price>[0-9,]+)\\s*$"
    );

    private static final Pattern KV_STYLE = Pattern.compile(
        "(?<key>listingType|species|item|price|quantity|seller|iv|fingerprint)\\s*[:=]\\s*(?<value>[^,;\\n]+)",
        Pattern.CASE_INSENSITIVE
    );

    private GTSMessageParser() {
    }

    public static Map<String, String> parse(String message) {
        if (message == null || message.trim().isEmpty()) {
            return null;
        }

        Matcher pokemonMatcher = GTS_POKEMON.matcher(message.trim());
        if (pokemonMatcher.matches()) {
            Map<String, String> parsed = new LinkedHashMap<>();
            parsed.put("listingType", "pokemon");
            parsed.put("species", pokemonMatcher.group("species").trim());
            parsed.put("level", pokemonMatcher.group("level").trim());
            parsed.put("iv", pokemonMatcher.group("iv").trim());
            parsed.put("price", normalizeNumber(pokemonMatcher.group("price")));
            parsed.put("fingerprint", ListingFingerprint.fingerprint((Map<?, ?>) parsed));
            return parsed;
        }

        Matcher itemMatcher = GTS_ITEM.matcher(message.trim());
        if (itemMatcher.matches()) {
            Map<String, String> parsed = new LinkedHashMap<>();
            parsed.put("listingType", "item");
            parsed.put("item", itemMatcher.group("item").trim());
            parsed.put("quantity", itemMatcher.group("qty").trim());
            parsed.put("price", normalizeNumber(itemMatcher.group("price")));
            parsed.put("fingerprint", ListingFingerprint.fingerprint((Map<?, ?>) parsed));
            return parsed;
        }

        Map<String, String> parsed = new LinkedHashMap<>();
        Matcher kvMatcher = KV_STYLE.matcher(message);
        while (kvMatcher.find()) {
            parsed.putIfAbsent(canonicalKey(kvMatcher.group("key")), kvMatcher.group("value").trim());
        }

        if (!looksLikeListing(parsed)) {
            return null;
        }

        parsed.replaceAll((key, value) -> normalize(value));
        parsed.putIfAbsent("fingerprint", ListingFingerprint.fingerprint((Map<?, ?>) parsed));
        return parsed;
    }

    private static String canonicalKey(String rawKey) {
        if (rawKey == null) {
            return "";
        }
        if (rawKey.equalsIgnoreCase("qty")) {
            return "quantity";
        }
        return rawKey;
    }

    private static boolean looksLikeListing(Map<String, String> parsed) {
        boolean hasType = parsed.containsKey("listingType") && !parsed.get("listingType").isBlank();
        boolean hasTarget = (parsed.containsKey("species") && !parsed.get("species").isBlank())
            || (parsed.containsKey("item") && !parsed.get("item").isBlank());
        return hasType && hasTarget;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.trim();
        if ((cleaned.startsWith("\"") && cleaned.endsWith("\""))
            || (cleaned.startsWith("'") && cleaned.endsWith("'"))) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }
        return cleaned;
    }

    private static String normalizeNumber(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^0-9-]", "");
    }
}
