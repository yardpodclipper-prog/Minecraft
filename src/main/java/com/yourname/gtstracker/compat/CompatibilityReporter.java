package com.yourname.gtstracker.compat;

import com.yourname.gtstracker.GTSTrackerMod;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Produces compatibility/runtime health summaries for command output and logs. */
public final class CompatibilityReporter {
    private CompatibilityReporter() {
    }

    public static CompatibilityReport evaluateRuntime() {
        FabricLoader loader = FabricLoader.getInstance();
        Optional<ModContainer> cobblemon = loader.getModContainer("cobblemon");
        Optional<ModContainer> fabricApi = loader.getModContainer("fabric-api");

        String cobblemonVersion = cobblemon.map(container -> version(container.getMetadata())).orElse("missing");
        String fabricApiVersion = fabricApi.map(container -> version(container.getMetadata())).orElse("missing");
        String loaderVersion = loader.getModContainer("fabricloader")
            .map(container -> version(container.getMetadata()))
            .orElse("unknown");
        String minecraftVersion = loader.getModContainer("minecraft")
            .map(container -> version(container.getMetadata()))
            .orElse("unknown");

        List<String> hardIncompatibilities = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (cobblemon.isEmpty()) {
            hardIncompatibilities.add("Cobblemon is missing; GTS parsing and interoperability cannot work.");
        }
        if (fabricApi.isEmpty()) {
            hardIncompatibilities.add("fabric-api is missing; client command/UI hooks will fail.");
        }
        if (Runtime.version().feature() < 21) {
            hardIncompatibilities.add("Java 21+ is required by GTSTracker.");
        }

        if (hardIncompatibilities.isEmpty() && !loader.isModLoaded("cobblemon")) {
            warnings.add("Cobblemon not loaded yet; verify load order.");
        }

        String summary = "MC=" + minecraftVersion
            + ", Loader=" + loaderVersion
            + ", FabricAPI=" + fabricApiVersion
            + ", Cobblemon=" + cobblemonVersion
            + ", Java=" + System.getProperty("java.version");

        return new CompatibilityReport(summary, List.copyOf(hardIncompatibilities), List.copyOf(warnings));
    }

    public static String summarizeRuntime() {
        return evaluateRuntime().summary();
    }

    public static void logStartupCompatibility() {
        CompatibilityReport report = evaluateRuntime();
        GTSTrackerMod.LOGGER.info("Runtime compatibility summary: {}", report.summary());

        for (String warning : report.warnings()) {
            GTSTrackerMod.LOGGER.warn("Compatibility warning: {}", warning);
        }

        for (String incompatibility : report.hardIncompatibilities()) {
            GTSTrackerMod.LOGGER.error("Hard compatibility failure: {}", incompatibility);
        }
    }

    private static String version(ModMetadata metadata) {
        return metadata.getVersion().getFriendlyString();
    }

    public record CompatibilityReport(String summary, List<String> hardIncompatibilities, List<String> warnings) {
        public boolean hasHardIncompatibilities() {
            return !hardIncompatibilities.isEmpty();
        }

        public String formatStatusLine() {
            if (hardIncompatibilities.isEmpty()) {
                return summary;
            }
            return String.format(Locale.ROOT, "%s | hard-incompatibilities=%s", summary, String.join("; ", hardIncompatibilities));
        }
    }
}
