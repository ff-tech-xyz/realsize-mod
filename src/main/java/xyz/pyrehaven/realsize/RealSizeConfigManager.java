package xyz.pyrehaven.realsize;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RealSizeConfigManager {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static volatile RealSizeConfig currentConfig = RealSizeConfig.defaults();

    private RealSizeConfigManager() {
    }

    public static RealSizeConfig currentConfig() {
        return currentConfig;
    }

    public static RealSizeConfig loadFromDisk() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("realsize.json");
        RealSizeConfig config = loadOrCreate(path);
        currentConfig = config;
        return config;
    }

    public static RealSizeConfig loadOrCreate(Path configFile) {
        RealSizeConfig defaults = RealSizeConfig.defaults();

        try {
            Files.createDirectories(configFile.getParent());

            if (Files.notExists(configFile)) {
                writeConfig(configFile, defaults);
                currentConfig = defaults;
                return defaults;
            }

            try (Reader reader = Files.newBufferedReader(configFile)) {
                RawRealSizeConfig rawConfig = GSON.fromJson(reader, RawRealSizeConfig.class);
                RealSizeConfig merged = mergeWithDefaults(rawConfig, defaults);
                currentConfig = merged;
                return merged;
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load RealSize config from " + configFile, exception);
        }
    }

    private static void writeConfig(Path configFile, RealSizeConfig config) throws IOException {
        try (Writer writer = Files.newBufferedWriter(configFile)) {
            GSON.toJson(config, writer);
        }
    }

    private static RealSizeConfig mergeWithDefaults(RawRealSizeConfig rawConfig, RealSizeConfig defaults) {
        if (rawConfig == null) {
            return defaults;
        }

        Map<String, Double> entityScales = new LinkedHashMap<>(defaults.entityScales());
        if (rawConfig.entityScales != null) {
            rawConfig.entityScales.forEach((entityId, scale) -> {
                if (entityId != null && !entityId.isBlank() && scale != null && scale > 0.0D) {
                    entityScales.put(entityId, scale);
                }
            });
        }

        double floor = positiveOrDefault(rawConfig.floor, defaults.floor());
        double cap = positiveOrDefault(rawConfig.cap, defaults.cap());
        if (cap < floor) {
            cap = floor;
        }

        double trackingRangeThreshold = positiveOrDefault(rawConfig.trackingRangeThreshold, defaults.trackingRangeThreshold());
        int minTrackingRangeChunks = positiveIntOrDefault(rawConfig.minTrackingRangeChunks, defaults.minTrackingRangeChunks());
        int minTrackingDistanceBlocks = positiveIntOrDefault(rawConfig.minTrackingDistanceBlocks, defaults.minTrackingDistanceBlocks());
        double stepHeightBoostThreshold = positiveOrDefault(rawConfig.stepHeightBoostThreshold, defaults.stepHeightBoostThreshold());
        double stepHeightBoostAmount = nonNegativeOrDefault(rawConfig.stepHeightBoostAmount, defaults.stepHeightBoostAmount());

        return new RealSizeConfig(
                floor,
                cap,
                trackingRangeThreshold,
                minTrackingRangeChunks,
                minTrackingDistanceBlocks,
                stepHeightBoostThreshold,
                stepHeightBoostAmount,
                entityScales
        );
    }

    private static double positiveOrDefault(Double value, double fallback) {
        return value != null && value > 0.0D ? value : fallback;
    }

    private static double nonNegativeOrDefault(Double value, double fallback) {
        return value != null && value >= 0.0D ? value : fallback;
    }

    private static int positiveIntOrDefault(Integer value, int fallback) {
        return value != null && value > 0 ? value : fallback;
    }

    private static final class RawRealSizeConfig {
        Double floor;
        Double cap;
        Double trackingRangeThreshold;
        Integer minTrackingRangeChunks;
        Integer minTrackingDistanceBlocks;
        Double stepHeightBoostThreshold;
        Double stepHeightBoostAmount;
        Map<String, Double> entityScales;
    }
}
