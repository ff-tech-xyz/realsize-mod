package xyz.pyrehaven.realsize.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public final class ConfigFile {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> V2_FIELDS = Set.of("configVersion", "floor", "hostileFloor", "cap",
            "proportionalStepHeight", "maxStepHeight", "clientRenderDistanceFix", "clientWalkTempoFix", "entityScales");
    private static final Set<String> OBSOLETE_FIELDS = Set.of("trackingRangeThreshold", "minTrackingRangeChunks",
            "minTrackingDistanceBlocks", "stepHeightBoostThreshold", "stepHeightBoostAmount");

    private ConfigFile() {
    }

    public static RealSizeConfig loadOrCreate(Path file, Consumer<String> diagnostics) throws IOException {
        return loadOrCreate(file, diagnostics, Function.identity());
    }

    // Resolve the complete candidate before any first-run or migration write.
    public static <T> T loadOrCreate(Path file, Consumer<String> diagnostics,
            Function<RealSizeConfig, T> validate) throws IOException {
        try {
            if (Files.isSymbolicLink(file)) {
                throw new IOException("$: symbolic-link config is not supported");
            }
            if (Files.notExists(file, LinkOption.NOFOLLOW_LINKS)) {
                RealSizeConfig defaults = DefaultScales.config();
                T candidate = validate.apply(defaults);
                replaceAtomically(file, defaults, null);
                return candidate;
            }
            byte[] original = Files.readAllBytes(file);
            String text = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(original)).toString();
            JsonObject object = parse(text);
            if (object.has("configVersion")) {
                return validate.apply(decode(object));
            }
            RealSizeConfig migrated = migrate(object);
            T candidate = validate.apply(migrated);
            replaceAtomically(file, migrated, original);
            diagnostics.accept(file + ": migrated unversioned legacy config to configVersion 2; original backup: "
                    + backupPath(file) + "; removed obsolete fields (no longer affect behavior): "
                    + String.join(", ", OBSOLETE_FIELDS));
            return candidate;
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            throw new IOException(file + ": " + exception.getMessage(), exception);
        }
    }

    private static Path backupPath(Path file) {
        return file.resolveSibling(file.getFileName() + ".legacy.bak");
    }

    private static void replaceAtomically(Path file, RealSizeConfig config, byte[] original) throws IOException {
        Path target = file.toAbsolutePath();
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), "." + target.getFileName() + ".", ".tmp");
        try {
            byte[] encoded = (GSON.toJson(config) + "\n").getBytes(StandardCharsets.UTF_8);
            writeForced(temporary, encoded, StandardOpenOption.TRUNCATE_EXISTING);
            if (original != null) {
                // Never overwrite an earlier migration backup, including a symlink or directory.
                writeForced(backupPath(file), original, StandardOpenOption.CREATE_NEW);
            }
            // No non-atomic fallback: unsupported filesystems must leave the original alone.
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void writeForced(Path file, byte[] bytes, StandardOpenOption creation) throws IOException {
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE, creation)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
        }
    }

    private static RealSizeConfig migrate(JsonObject object) {
        for (String field : object.keySet()) {
            if (!Set.of("floor", "cap", "entityScales").contains(field) && !OBSOLETE_FIELDS.contains(field)) {
                throw new IllegalArgumentException("configVersion: missing; " + field + ": not a legacy field");
            }
        }
        for (String field : OBSOLETE_FIELDS) {
            if (!object.has(field)) {
                continue;
            }
            double value = numeric(object, field);
            boolean zeroAllowed = field.equals("stepHeightBoostAmount");
            if (!Double.isFinite(value) || (zeroAllowed ? value < 0 : value <= 0)) {
                throw new IllegalArgumentException(field + ": invalid legacy value");
            }
            if (field.startsWith("minTracking") && (value != Math.rint(value) || value > Integer.MAX_VALUE)) {
                throw new IllegalArgumentException(field + ": expected positive integer");
            }
        }
        double floor = number(object, "floor");
        return new RealSizeConfig(2, floor, floor, number(object, "cap"),
                true, 1.49, true, true, scales(object));
    }

    private static JsonObject parse(String text) throws IOException {
        try (var reader = new JsonReader(new StringReader(text))) {
            reader.setStrictness(Strictness.STRICT);
            JsonObject object = readObject(reader, 0);
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw new IOException("$: expected end of document");
            }
            return object;
        }
    }

    // A two-level schema, read without Gson's duplicate-key or scalar-coercion defaults.
    private static JsonObject readObject(JsonReader reader, int depth) throws IOException {
        if (depth > 1) {
            throw new IOException(reader.getPath() + ": unexpected nested object");
        }
        reader.beginObject();
        JsonObject object = new JsonObject();
        while (reader.hasNext()) {
            String key = reader.nextName();
            if (object.has(key)) {
                throw new IOException(reader.getPath() + ": duplicate field");
            }
            JsonElement value = switch (reader.peek()) {
                case BEGIN_OBJECT -> readObject(reader, depth + 1);
                case NUMBER -> {
                    String field = reader.getPath();
                    try {
                        yield new JsonPrimitive(new BigDecimal(reader.nextString()));
                    } catch (NumberFormatException exception) {
                        throw new IOException(field + ": number is outside supported decimal range", exception);
                    }
                }
                case BOOLEAN -> new JsonPrimitive(reader.nextBoolean());
                case STRING -> new JsonPrimitive(reader.nextString());
                case NULL -> {
                    reader.nextNull();
                    yield JsonNull.INSTANCE;
                }
                default -> throw new IOException(reader.getPath() + ": unexpected value");
            };
            object.add(key, value);
        }
        reader.endObject();
        return object;
    }

    private static RealSizeConfig decode(JsonObject object) {
        for (String field : object.keySet()) {
            if (!V2_FIELDS.contains(field)) {
                throw new IllegalArgumentException(field + ": unknown field");
            }
        }
        JsonElement version = required(object, "configVersion");
        if (!version.isJsonPrimitive() || !version.getAsJsonPrimitive().isNumber()
                || version.getAsBigDecimal().compareTo(BigDecimal.valueOf(2)) != 0) {
            throw new IllegalArgumentException("configVersion: expected 2");
        }
        return new RealSizeConfig(2, number(object, "floor"), number(object, "hostileFloor"),
                number(object, "cap"), bool(object, "proportionalStepHeight"),
                number(object, "maxStepHeight"), bool(object, "clientRenderDistanceFix"),
                !object.has("clientWalkTempoFix") || bool(object, "clientWalkTempoFix"), scales(object));
    }

    private static Map<String, Double> scales(JsonObject object) {
        JsonElement value = required(object, "entityScales");
        if (!value.isJsonObject()) {
            throw new IllegalArgumentException("entityScales: expected object");
        }
        Map<String, Double> scales = new LinkedHashMap<>();
        value.getAsJsonObject().keySet().forEach(selector -> {
            double scale = number(value.getAsJsonObject(), selector);
            scales.put(selector, scale);
        });
        return scales;
    }

    private static JsonElement required(JsonObject object, String field) {
        JsonElement value = object.get(field);
        if (value == null || value.isJsonNull()) {
            throw new IllegalArgumentException(field + ": required field");
        }
        return value;
    }

    private static double number(JsonObject object, String field) {
        double number = numeric(object, field);
        if (!Double.isFinite(number) || number <= 0) {
            throw new IllegalArgumentException(field + ": must be positive and finite");
        }
        return number;
    }

    private static double numeric(JsonObject object, String field) {
        JsonElement value = required(object, field);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(field + ": expected number");
        }
        return value.getAsDouble();
    }

    private static boolean bool(JsonObject object, String field) {
        JsonElement value = required(object, field);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException(field + ": expected boolean");
        }
        return value.getAsBoolean();
    }
}
