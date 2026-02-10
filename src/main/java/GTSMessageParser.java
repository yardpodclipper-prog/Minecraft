import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses listing-like messages into a normalized map.
 */
public class GTSMessageParser {
    private static final String[] SUPPORTED_KEYS = {
        "listingType", "species", "item", "price", "quantity", "seller", "iv", "fingerprint"
    };

    private static final Pattern JSON_STYLE = Pattern.compile(
        "\"(?<key>listingType|species|item|price|quantity|seller|iv|fingerprint)\"\\s*:\\s*(\"(?<text>[^\"]*)\"|(?<number>-?\\d+(?:\\.\\d+)?))",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern KV_STYLE = Pattern.compile(
        "(?<key>listingType|species|item|price|quantity|seller|iv|fingerprint)\\s*[:=]\\s*(?<value>[^,;\\n]+)",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Parses a message and returns an empty map for non-listings.
     */
    public Map<String, String> parse(String message) {
        if (message == null || message.trim().isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> parsed = new LinkedHashMap<>();
        extractJsonStyle(message, parsed);
        extractKeyValueStyle(message, parsed);

        for (String key : SUPPORTED_KEYS) {
            String value = parsed.get(key);
            if (value != null) {
                parsed.put(key, normalize(value));
            }
        }

        if (!looksLikeListing(parsed)) {
            return Collections.emptyMap();
        }

        if (!parsed.containsKey("fingerprint") || parsed.get("fingerprint").isEmpty()) {
            parsed.put("fingerprint", ListingFingerprint.fingerprint(buildCanonicalFingerprintSource(parsed)));
        }

        return parsed;
    }

    private static void extractJsonStyle(String message, Map<String, String> target) {
        Matcher matcher = JSON_STYLE.matcher(message);
        while (matcher.find()) {
            String key = canonicalKey(matcher.group("key"));
            if (!target.containsKey(key)) {
                String text = matcher.group("text");
                String number = matcher.group("number");
                target.put(key, text != null ? text : number);
            }
        }
    }

    private static void extractKeyValueStyle(String message, Map<String, String> target) {
        Matcher matcher = KV_STYLE.matcher(message);
        while (matcher.find()) {
            String key = canonicalKey(matcher.group("key"));
            if (!target.containsKey(key)) {
                target.put(key, matcher.group("value"));
            }
        }
    }

    private static String canonicalKey(String rawKey) {
        if (rawKey == null) {
            return "";
        }
        for (String key : SUPPORTED_KEYS) {
            if (key.equalsIgnoreCase(rawKey)) {
                return key;
            }
        }
        return rawKey;
    }

    private static boolean looksLikeListing(Map<String, String> parsed) {
        boolean hasType = parsed.containsKey("listingType") && !parsed.get("listingType").isEmpty();
        boolean hasTarget = (parsed.containsKey("species") && !parsed.get("species").isEmpty())
            || (parsed.containsKey("item") && !parsed.get("item").isEmpty());
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

    private static String buildCanonicalFingerprintSource(Map<String, String> parsed) {
        StringBuilder builder = new StringBuilder();
        for (String key : SUPPORTED_KEYS) {
            if ("fingerprint".equals(key)) {
                continue;
            }
            if (parsed.containsKey(key)) {
                builder.append(key).append('=').append(parsed.get(key)).append('|');
            }
        }
        return builder.toString();
    }
}
