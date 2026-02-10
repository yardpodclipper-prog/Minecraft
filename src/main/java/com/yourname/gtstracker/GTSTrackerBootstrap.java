package com.yourname.gtstracker;

import java.util.Objects;

/**
 * Wires and controls lifecycle for core services.
 */
public final class GTSTrackerBootstrap {
    private final GTSParser parser;
    private final FingerprintService fingerprintService;
    private final DatabaseManager databaseManager;

    public GTSTrackerBootstrap(
            GTSParser parser,
            FingerprintService fingerprintService,
            DatabaseManager databaseManager
    ) {
        this.parser = Objects.requireNonNull(parser, "parser");
        this.fingerprintService = Objects.requireNonNull(fingerprintService, "fingerprintService");
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager");
    }

    public static GTSTrackerBootstrap createDefault() {
        DatabaseManager databaseManager = new DatabaseManager("jdbc:sqlite:gts-tracker.db");
        GTSParser parser = new GTSParser();
        FingerprintService fingerprintService = new FingerprintService(parser);
        return new GTSTrackerBootstrap(parser, fingerprintService, databaseManager);
    }

    public void start() {
        databaseManager.initialize();
    }

    public void stop() {
        databaseManager.close();
    }

    public GTSParser getParser() {
        return parser;
    }

    public FingerprintService getFingerprintService() {
        return fingerprintService;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
