package com.yourname.gtstracker;

/**
 * Simple parser placeholder for GTS data.
 */
public final class GTSParser {
    public ParsedRecord parse(String rawValue) {
        return new ParsedRecord(rawValue == null ? "" : rawValue.trim());
    }

    public record ParsedRecord(String normalizedValue) {
    }
}
