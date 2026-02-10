import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight listing persistence with upsert-by-fingerprint behavior.
 */
public class DatabaseManager {
    private final String location;
    private final GTSMessageParser parser;
    private final Map<String, Map<String, String>> byFingerprint;

    public DatabaseManager(String dbPathOrJdbcUrl) {
        this.location = dbPathOrJdbcUrl == null ? "" : dbPathOrJdbcUrl.trim();
        this.parser = new GTSMessageParser();
        this.byFingerprint = new ConcurrentHashMap<>();
    }

    /**
     * Parses and upserts a listing message. Non-listings are ignored.
     *
     * @return true when a listing was inserted/updated, false otherwise
     */
    public boolean upsert(String listingMessage) {
        Map<String, String> listing = parser.parse(listingMessage);
        if (listing.isEmpty()) {
            return false;
        }

        String fingerprint = listing.get("fingerprint");
        if (fingerprint == null || fingerprint.isEmpty()) {
            fingerprint = ListingFingerprint.fingerprint(listingMessage);
            listing.put("fingerprint", fingerprint);
        }

        byFingerprint.put(fingerprint, new LinkedHashMap<>(listing));
        persistRecord(byFingerprint.get(fingerprint));
        return true;
    }

    public Map<String, Map<String, String>> getAll() {
        return Collections.unmodifiableMap(byFingerprint);
    }

    private void persistRecord(Map<String, String> listing) {
        if (location.isEmpty() || location.toLowerCase().startsWith("jdbc:")) {
            return;
        }

        Path output = Paths.get(location);
        try {
            Path parent = output.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            String line = toStorageLine(listing) + System.lineSeparator();
            Files.write(
                output,
                line.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist listing to " + location, e);
        }
    }

    private static String toStorageLine(Map<String, String> listing) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : listing.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            first = false;
            builder.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return builder.toString();
    }
}
