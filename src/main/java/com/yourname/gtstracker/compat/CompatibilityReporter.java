package com.yourname.gtstracker.compat;

import com.yourname.gtstracker.GTSTrackerMod;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;

import java.util.Optional;

/** Produces compatibility/runtime health summaries for command output and logs. */
public final class CompatibilityReporter {
    private CompatibilityReporter() {
    }

    public static String summarizeRuntime() {
        FabricLoader loader = FabricLoader.getInstance();
        Optional<ModContainer> cobblemon = loader.getModContainer("cobblemon");
        Optional<ModContainer> fabricApi = loader.getModContainer("fabric-api");

        String cobblemonVersion = cobblemon.map(container -> version(container.getMetadata())).orElse("missing");
        String fabricApiVersion = fabricApi.map(container -> version(container.getMetadata())).orElse("missing");
        String loaderVersion = loader.getModContainer("fabricloader")
            .map(container -> version(container.getMetadata()))
            .orElse("unknown");

        return "MC=" + loader.getModContainer("minecraft").map(container -> version(container.getMetadata())).orElse("unknown")
            + ", Loader=" + loaderVersion
            + ", FabricAPI=" + fabricApiVersion
            + ", Cobblemon=" + cobblemonVersion
            + ", Java=" + System.getProperty("java.version");
    }

    public static void logStartupCompatibility() {
        String summary = summarizeRuntime();
        GTSTrackerMod.LOGGER.info("Runtime compatibility summary: {}", summary);

        if (FabricLoader.getInstance().getModContainer("cobblemon").isEmpty()) {
            GTSTrackerMod.LOGGER.warn("Cobblemon is not present. GTSTracker can load, but GTS interoperability will be limited.");
        }
    }

    private static String version(ModMetadata metadata) {
        return metadata.getVersion().getFriendlyString();
    }
}
