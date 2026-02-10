package com.yourname.gtstracker;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Creates deterministic fingerprints from parsed records.
 */
public final class FingerprintService {
    private final GTSParser parser;

    public FingerprintService(GTSParser parser) {
        this.parser = Objects.requireNonNull(parser, "parser");
    }

    public String fingerprint(String rawValue) {
        GTSParser.ParsedRecord parsed = parser.parse(rawValue);
        return sha256(parsed.normalizedValue());
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }
}
