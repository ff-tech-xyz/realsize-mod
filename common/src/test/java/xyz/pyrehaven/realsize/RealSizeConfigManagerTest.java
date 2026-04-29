package xyz.pyrehaven.realsize;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RealSizeConfigManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void loadFromConfigDirWritesDefaultConfigWhenMissing() throws Exception {
        RealSizeConfig config = RealSizeConfigManager.loadFromConfigDir(tempDir);
        Path configFile = tempDir.resolve("realsize.json");

        assertTrue(Files.exists(configFile));
        assertEquals(0.22, config.floor());
        assertEquals(1.45, config.cap());
        assertEquals(0.60, config.trackingRangeThreshold());
        assertEquals(10, config.minTrackingRangeChunks());
        assertEquals(128, config.minTrackingDistanceBlocks());
        assertEquals(1.10, config.stepHeightBoostThreshold());
        assertEquals(0.5, config.stepHeightBoostAmount());
        assertEquals(0.25, config.scaleFor("minecraft:bee", 1.0));
        assertEquals(0.40, config.scaleFor("minecraft:nautilus", 1.0));
        assertEquals(0.40, config.scaleFor("minecraft:zombie_nautilus", 1.0));

        String json = Files.readString(configFile);
        assertTrue(json.contains("\"minecraft:bee\": 0.25"));
        assertTrue(json.contains("\"minecraft:nautilus\": 0.4"));
        assertTrue(json.contains("\"minecraft:zombie_nautilus\": 0.4"));
    }

    @Test
    void loadOrCreateMergesCustomValuesWithDefaults() throws Exception {
        Path configFile = tempDir.resolve("realsize.json");
        Files.writeString(configFile, """
                {
                  "cap": 1.3,
                  "trackingRangeThreshold": 0.5,
                  "entityScales": {
                    "minecraft:bee": 0.3
                  }
                }
                """);

        RealSizeConfig config = RealSizeConfigManager.loadOrCreate(configFile);

        assertEquals(0.22, config.floor());
        assertEquals(1.3, config.cap());
        assertEquals(0.5, config.trackingRangeThreshold());
        assertEquals(10, config.minTrackingRangeChunks());
        assertEquals(128, config.minTrackingDistanceBlocks());
        assertEquals(0.3, config.scaleFor("minecraft:bee", 1.0));
        assertEquals(0.40, config.scaleFor("minecraft:nautilus", 1.0));
    }

    @Test
    void logicHelpersUseConfiguredValues() {
        RealSizeConfig config = RealSizeConfig.defaults();

        assertEquals(0.22, RealSizeLogic.clampScale(0.01, config));
        assertEquals(1.45, RealSizeLogic.clampScale(10.0, config));
        assertEquals(0.5, RealSizeLogic.scaleForEntity("minecraft:rabbit", 0.5, config));
        assertEquals(1.0, RealSizeLogic.scaleForEntity("minecraft:creeper", 1.0, config));
        assertTrue(RealSizeLogic.shouldBoostTracking("minecraft:bee", 1.0, config));
        assertFalse(RealSizeLogic.shouldBoostTracking("minecraft:horse", 1.05, config));
        assertTrue(RealSizeLogic.shouldBoostStepHeight("minecraft:elder_guardian", 1.35, config));
        assertFalse(RealSizeLogic.shouldBoostStepHeight("minecraft:bee", 1.0, config));
    }

    @Test
    void loadOrCreateIgnoresInvalidValuesAndNullEntityScaleEntries() throws Exception {
        Path configFile = tempDir.resolve("realsize.json");
        Files.writeString(configFile, """
                {
                  "floor": -1.0,
                  "cap": 0.1,
                  "trackingRangeThreshold": -0.5,
                  "minTrackingRangeChunks": 0,
                  "minTrackingDistanceBlocks": -32,
                  "stepHeightBoostThreshold": -3.0,
                  "stepHeightBoostAmount": -0.25,
                  "entityScales": {
                    "minecraft:bee": null,
                    "": 0.9,
                    "minecraft:fox": -2.0,
                    "minecraft:rabbit": 0.6
                  }
                }
                """);

        RealSizeConfig config = RealSizeConfigManager.loadOrCreate(configFile);

        assertEquals(0.22, config.floor());
        assertEquals(0.22, config.cap());
        assertEquals(0.60, config.trackingRangeThreshold());
        assertEquals(10, config.minTrackingRangeChunks());
        assertEquals(128, config.minTrackingDistanceBlocks());
        assertEquals(1.10, config.stepHeightBoostThreshold());
        assertEquals(0.5, config.stepHeightBoostAmount());
        assertEquals(0.25, config.scaleFor("minecraft:bee", 1.0));
        assertEquals(0.50, config.scaleFor("minecraft:fox", 1.0));
        assertEquals(0.6, config.scaleFor("minecraft:rabbit", 1.0));
    }
}
