import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Generates deterministic listing fingerprints.
 */
public final class ListingFingerprint {
    private ListingFingerprint() {
    }

    /**
     * Creates a deterministic fingerprint for any single input string.
     *
     * @param source source value to hash
     * @return lowercase hex SHA-256 hash, or empty hash for null input
     */
    public static String fingerprint(String source) {
        String normalized = source == null ? "" : source.trim().toLowerCase();
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
}
