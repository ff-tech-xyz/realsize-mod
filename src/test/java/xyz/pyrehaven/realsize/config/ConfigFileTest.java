package xyz.pyrehaven.realsize.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ConfigFileTest {
    @TempDir Path directory;

    private static final String VALID = """
            {"configVersion":2,"floor":0.22,"hostileFloor":0.6,"cap":1.45,
             "proportionalStepHeight":true,"maxStepHeight":1.49,
             "clientRenderDistanceFix":true,"entityScales":{"minecraft:bee":0.25}}
            """;

    private static final String LEGACY = """
            {"floor":0.22,"cap":1.45,"trackingRangeThreshold":0.6,
             "minTrackingRangeChunks":10,"minTrackingDistanceBlocks":128,
             "stepHeightBoostThreshold":1.1,"stepHeightBoostAmount":0.5,
             "entityScales":{"minecraft:bee":0.4,"example:unknown":0.2}}
            """;

    @Test
    void walkTempoOptionAcceptsExplicitBooleanWithoutRewritingV2() throws Exception {
        Path file = directory.resolve("realsize.json");
        var object = JsonParser.parseString(VALID).getAsJsonObject();
        object.addProperty("clientWalkTempoFix", false);
        byte[] original = object.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(file, original);
        assertFalse(ConfigFile.loadOrCreate(file, ignored -> {}).clientWalkTempoFix());
        assertArrayEquals(original, Files.readAllBytes(file));
        Files.writeString(file, VALID);
        assertTrue(ConfigFile.loadOrCreate(file, ignored -> {}).clientWalkTempoFix());
        assertEquals(VALID, Files.readString(file), "Old v2 files must not be rewritten to add the default");
        for (String bad : new String[]{"null", "0", "\"false\"", "{}"}) {
            String invalid = VALID.replace("\"entityScales\":", "\"clientWalkTempoFix\":" + bad + ",\"entityScales\":");
            Files.writeString(file, invalid);
            assertTrue(assertThrows(IOException.class,
                    () -> ConfigFile.loadOrCreate(file, ignored -> {})).getMessage().contains("clientWalkTempoFix"));
            assertEquals(invalid, Files.readString(file));
        }
    }

    @Test
    void firstRunPreservesLegacySmallHostileSizes() throws Exception {
        var config = ConfigFile.loadOrCreate(directory.resolve("realsize.json"), ignored -> {});
        assertEquals(0.22, config.floor());
        assertEquals(config.floor(), config.hostileFloor(), "A hostile floor must not silently enlarge v1 defaults");
        var legacy = java.util.Map.of("minecraft:spider", 0.26, "minecraft:cave_spider", 0.20,
                "minecraft:silverfish", 0.22, "minecraft:endermite", 0.22,
                "minecraft:zombie_nautilus", 0.40, "minecraft:vex", 0.30);
        legacy.forEach((id, scale) -> assertEquals(scale, config.entityScales().get(id), id));
        assertEquals(config, ConfigFile.loadOrCreate(directory.resolve("realsize.json"), ignored -> {}));
    }

    @Test
    void legacyMigrationBacksUpExactBytesAndDoesNotMergeDefaults() throws Exception {
        Path file = directory.resolve("realsize.json");
        byte[] original = (" \r\n" + LEGACY + "\t\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(file, original);
        var messages = new ArrayList<String>();
        RealSizeConfig migrated;
        try (var originalHandle = java.nio.channels.FileChannel.open(file)) {
            migrated = ConfigFile.loadOrCreate(file, messages::add);
            var stillOriginal = java.nio.ByteBuffer.allocate(original.length);
            while (stillOriginal.hasRemaining()) {
                assertTrue(originalHandle.read(stillOriginal) > 0);
            }
            assertArrayEquals(original, stillOriginal.array(), "Replacement must not truncate the original inode");
        }
        assertEquals(2, migrated.configVersion());
        assertEquals(0.22, migrated.floor());
        assertEquals(1.45, migrated.cap());
        assertEquals(0.22, migrated.hostileFloor());
        assertTrue(migrated.proportionalStepHeight());
        assertEquals(1.49, migrated.maxStepHeight());
        assertEquals(java.util.Map.of("minecraft:bee", 0.4, "example:unknown", 0.2), migrated.entityScales());
        Path backup = directory.resolve("realsize.json.legacy.bak");
        assertArrayEquals(original, Files.readAllBytes(backup));
        assertEquals(1, messages.size());
        for (String name : new String[]{file.toString(), backup.toString(), "configVersion", "trackingRangeThreshold",
                "minTrackingRangeChunks", "minTrackingDistanceBlocks", "stepHeightBoostThreshold", "stepHeightBoostAmount"}) {
            assertTrue(messages.getFirst().contains(name), messages.getFirst());
        }
        String updated = Files.readString(file);
        assertFalse(updated.contains("trackingRangeThreshold"));
        messages.clear();
        assertEquals(migrated, ConfigFile.loadOrCreate(file, messages::add));
        assertTrue(messages.isEmpty());
        assertEquals(updated, Files.readString(file));
        assertArrayEquals(original, Files.readAllBytes(backup));
    }

    @Test
    void legacyEmptyMapStaysEmptyAndMissingCoreFieldsFail() throws Exception {
        Path file = directory.resolve("realsize.json");
        Files.writeString(file, "{\"floor\":0.7,\"cap\":1.4,\"entityScales\":{}}");
        var migrated = ConfigFile.loadOrCreate(file, ignored -> {});
        assertTrue(migrated.entityScales().isEmpty());
        assertEquals(0.7, migrated.hostileFloor());
        Files.delete(directory.resolve("realsize.json.legacy.bak"));
        for (String field : new String[]{"floor", "cap", "entityScales"}) {
            var input = JsonParser.parseString(LEGACY).getAsJsonObject();
            input.remove(field);
            assertInvalid(input.toString(), field);
        }
        assertInvalid(LEGACY.replace("1.45", "0.1"), "cap");
        assertInvalid(LEGACY.replace("\"stepHeightBoostAmount\":0.5", "\"stepHeightBoostAmount\":-1"),
                "stepHeightBoostAmount");
        assertInvalid(LEGACY.replace("\"minTrackingRangeChunks\":10", "\"minTrackingRangeChunks\":1.5"),
                "minTrackingRangeChunks");
        assertInvalid(LEGACY.replace("0.4", "0"), "minecraft:bee");
    }

    @Test
    void existingBackupStopsMigrationWithoutChangingEitherFileOrLeavingTemps() throws Exception {
        Path file = directory.resolve("realsize.json");
        Path backup = directory.resolve("realsize.json.legacy.bak");
        Files.writeString(file, LEGACY);
        Files.writeString(backup, "precious original");
        var messages = new ArrayList<String>();
        var error = assertThrows(IOException.class, () -> ConfigFile.loadOrCreate(file, messages::add));
        assertTrue(error.getMessage().contains(file.toString()), error.getMessage());
        assertTrue(error.getMessage().contains("legacy.bak"), error.getMessage());
        assertEquals(LEGACY, Files.readString(file));
        assertEquals("precious original", Files.readString(backup));
        assertTrue(messages.isEmpty());
        try (var entries = Files.list(directory)) {
            assertEquals(2, entries.count());
        }
        Files.delete(backup);
        Files.createDirectory(backup);
        assertThrows(IOException.class, () -> ConfigFile.loadOrCreate(file, messages::add));
        assertEquals(LEGACY, Files.readString(file));
    }

    @Test
    void symlinkIsNotTreatedAsAMissingOrReplaceableConfig() throws Exception {
        Path file = directory.resolve("realsize.json");
        Path target = directory.resolve("missing.json");
        Files.createSymbolicLink(file, target);
        var error = assertThrows(IOException.class, () -> ConfigFile.loadOrCreate(file, ignored -> {}));
        assertTrue(error.getMessage().contains(file.toString()));
        assertTrue(Files.isSymbolicLink(file));
        assertFalse(Files.exists(target));
    }

    @Test
    void unwritableDirectoryFailsMigrationWithoutChangingOriginal() throws Exception {
        Path file = directory.resolve("realsize.json");
        Files.writeString(file, LEGACY);
        var permissions = Files.getPosixFilePermissions(directory);
        try {
            Files.setPosixFilePermissions(directory, java.nio.file.attribute.PosixFilePermissions.fromString("r-x------"));
            var error = assertThrows(IOException.class, () -> ConfigFile.loadOrCreate(file, ignored -> {}));
            assertTrue(error.getMessage().contains(file.toString()), error.getMessage());
            assertEquals(LEGACY, Files.readString(file));
            assertFalse(Files.exists(directory.resolve("realsize.json.legacy.bak")));
        } finally {
            Files.setPosixFilePermissions(directory, permissions);
        }
    }

    @Test
    void snapshotDefensivelyCopiesCallerMap() {
        var entries = new java.util.HashMap<String, Double>();
        entries.put("minecraft:bee", 0.3);
        var config = new RealSizeConfig(2, 0.22, 0.6, 1.45, true, 1.49, true, true, entries);
        entries.clear();
        assertEquals(java.util.Map.of("minecraft:bee", 0.3), config.entityScales());
        assertThrows(IllegalArgumentException.class,
                () -> new RealSizeConfig(2, 0.22, 0.6, 1.45, true, 1.49, true, true,
                        java.util.Map.of("minecraft:bee", Double.NaN)));
    }

    @Test
    void schemaVersionCannotBeRoundedToTwo() throws Exception {
        assertInvalid(VALID.replace("\"configVersion\":2", "\"configVersion\":2.0000000000000001"), "configVersion");
    }

    @Test
    void unsupportedDecimalExponentNamesTheFieldWithoutChangingTheFile() throws Exception {
        assertInvalid(VALID.replace("0.25", "1e-2147483648"), "minecraft:bee");
        assertInvalid(VALID.replace("0.22", "1e2147483648"), "floor");
    }

    @Test
    void everyV2FieldIsRequiredWithoutRewritingTheFile() throws Exception {
        for (String field : JsonParser.parseString(VALID).getAsJsonObject().keySet()) {
            // Missing version alone is not a legacy file: v2-only keys prevent migration.
            JsonObject input = JsonParser.parseString(VALID).getAsJsonObject();
            input.remove(field);
            assertInvalid(input.toString(), field.equals("configVersion") ? "configVersion" : field);
        }
    }

    @Test
    void rejectsMalformedAndInvalidValuesWithNamedDiagnosticsAndExactBytes() throws Exception {
        for (String value : new String[]{"0", "-0.1", "1e999", "null", "\"0.3\"", "true", "[]"}) {
            assertInvalid(VALID.replace("0.25", value), "minecraft:bee");
        }
        for (String field : new String[]{"floor", "hostileFloor", "cap", "maxStepHeight"}) {
            JsonObject input = JsonParser.parseString(VALID).getAsJsonObject();
            input.addProperty(field, -1);
            assertInvalid(input.toString(), field);
        }
        assertInvalid(VALID.replace("\"hostileFloor\":0.6", "\"hostileFloor\":0.1"), "hostileFloor");
        assertInvalid(VALID.replace("\"cap\":1.45", "\"cap\":0.4"), "cap");
        assertInvalid(VALID.replace("1.49", "1.5"), "maxStepHeight");
        assertInvalid(VALID.replace("\"configVersion\":2", "\"configVersion\":2.1"), "configVersion");
        assertInvalid(VALID.replace("\"configVersion\":2", "\"configVersion\":99"), "configVersion");
        assertInvalid(VALID.replace("true", "\"true\""), "proportionalStepHeight");
        assertInvalid(VALID.replace("minecraft:bee", "Minecraft:Bee"), "Minecraft:Bee");
        assertInvalid(VALID.replace("minecraft:bee", "bee"), "bee");
        assertInvalid(VALID.replace("\"floor\":0.22", "\"floor\":0.22,\"floor\":0.3"), "floor");
        assertInvalid(VALID.replace("0.25", "NaN"), "minecraft:bee");
        assertInvalid(VALID.replace("0.25", "Infinity"), "minecraft:bee");
        assertInvalid(VALID.replace("0.25", "0.25,\"minecraft:bee\":0.5"), "minecraft:bee");
        assertInvalid(VALID.replace("\"cap\"", "\"cpa\""), "cpa");
        assertInvalid(VALID + "{}", "$");
        assertInvalid("null", "$");
        assertInvalid("", "$");
        assertInvalid("{broken", "$");
        assertInvalid(VALID.replace("\"floor\"", "floor"), "$");
    }

    @Test
    void deletedDefaultsAndUnknownWellFormedSelectorsAreRetainedWithoutRewriting() throws Exception {
        String custom = VALID.replace("\"minecraft:bee\":0.25",
                "\"example:unknown_mob\":0.3,\"#example:unknown_tag\":0.4,\"minecraft:horse\":1.0");
        Path file = directory.resolve("realsize.json");
        Files.writeString(file, custom);
        var config = ConfigFile.loadOrCreate(file, ignored -> {});
        assertEquals(3, config.entityScales().size());
        assertFalse(config.entityScales().containsKey("minecraft:bee"));
        assertEquals(0.4, config.entityScales().get("#example:unknown_tag"));
        assertEquals(custom, Files.readString(file));
    }

    private void assertInvalid(String input, String field) throws Exception {
        Path file = directory.resolve("realsize.json");
        byte[] original = (" \n" + input + "\n ").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(file, original);
        IOException error = assertThrows(IOException.class,
                () -> ConfigFile.loadOrCreate(file, ignored -> {}), input);
        assertTrue(error.getMessage().contains(file.toString()), error.getMessage());
        assertTrue(error.getMessage().contains(field), error.getMessage());
        assertArrayEquals(original, Files.readAllBytes(file));
        assertFalse(Files.exists(directory.resolve("realsize.json.legacy.bak")));
    }

    @Test
    void missingFileCreatesDefaultsButExistingMapRemainsAuthoritative() throws Exception {
        Path file = directory.resolve("nested/realsize.json");
        var messages = new ArrayList<String>();
        var generated = ConfigFile.loadOrCreate(file, messages::add);
        assertEquals(0.22, generated.floor());
        assertEquals(0.22, generated.hostileFloor());
        assertEquals(1.45, generated.cap());
        assertEquals(1.49, generated.maxStepHeight());
        assertTrue(generated.proportionalStepHeight());
        assertTrue(generated.clientRenderDistanceFix());
        assertEquals(0.25, generated.entityScales().get("minecraft:bee"));
        assertEquals(generated, ConfigFile.loadOrCreate(file, messages::add));
        assertThrows(UnsupportedOperationException.class,
                () -> generated.entityScales().put("minecraft:bee", 0.4));

        String custom = """
                {"configVersion":2,"floor":0.22,"hostileFloor":0.6,"cap":1.45,
                 "proportionalStepHeight":false,"maxStepHeight":1.49,
                 "clientRenderDistanceFix":false,"entityScales":{}}
                """;
        Files.writeString(file, custom);
        var empty = ConfigFile.loadOrCreate(file, messages::add);
        assertTrue(empty.entityScales().isEmpty());
        assertFalse(empty.proportionalStepHeight());
        assertFalse(empty.clientRenderDistanceFix());
        assertEquals(custom, Files.readString(file));
    }
}
