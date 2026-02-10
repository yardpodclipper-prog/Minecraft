package com.yourname.gtstracker;

import net.fabricmc.api.ModInitializer;

/**
 * Fabric entrypoint for GTS Tracker.
 */
public final class GTSTrackerMod implements ModInitializer {
    public static final String MOD_ID = "gtstracker";

    private GTSTrackerBootstrap bootstrap;

    @Override
    public void onInitialize() {
        bootstrap = GTSTrackerBootstrap.createDefault();
        bootstrap.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (bootstrap != null) {
                bootstrap.stop();
            }
        }, MOD_ID + "-shutdown"));
    }
}
