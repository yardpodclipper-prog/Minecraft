package com.yourname.gtstracker;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles DB setup and teardown.
 */
public final class DatabaseManager implements AutoCloseable {
    private final String jdbcUrl;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public DatabaseManager(String jdbcUrl) {
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl, "jdbcUrl");
    }

    public void initialize() {
        initialized.set(true);
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    @Override
    public void close() {
        initialized.set(false);
    }
}
