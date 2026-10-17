package xyz.pyrehaven.realsize;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

class ScaffoldTest {
    @Test
    void packagedModIsInstallableInBothEnvironments() throws Exception {
        Path artifact = Path.of(System.getProperty("realsize.test.jar"));
        Path project = Path.of(System.getProperty("realsize.test.project"));
        assertTrue(Files.isRegularFile(artifact), "The build must produce an installable JAR");

        try (ZipFile jar = new ZipFile(artifact.toFile())) {
            var metadata = jar.getEntry("fabric.mod.json");
            assertNotNull(metadata, "Fabric Loader must be able to discover the mod");
            JsonObject mod;
            try (var reader = new InputStreamReader(jar.getInputStream(metadata), StandardCharsets.UTF_8)) {
                mod = JsonParser.parseReader(reader).getAsJsonObject();
            }
            assertEquals("realsize", mod.get("id").getAsString());
            assertEquals("1.2.0", mod.get("version").getAsString());
            assertEquals("Configurable mob sizes and proportional land movement, with an optional client render-distance correction.",
                    mod.get("description").getAsString());
            assertEquals("*", mod.get("environment").getAsString());
            assertEquals("26.3", mod.getAsJsonObject("depends").get("minecraft").getAsString());
            assertEquals(">=25", mod.getAsJsonObject("depends").get("java").getAsString());
            assertEquals(">=0.19.5", mod.getAsJsonObject("depends").get("fabricloader").getAsString());
            assertEquals(">=0.162.0+26.3", mod.getAsJsonObject("depends").get("fabric-api").getAsString());
            assertEquals("https://github.com/ff-tech-xyz/realsize-mod",
                    mod.getAsJsonObject("contact").get("sources").getAsString());
            assertEquals("https://github.com/ff-tech-xyz/realsize-mod/issues",
                    mod.getAsJsonObject("contact").get("issues").getAsString());
            assertEquals("MIT", mod.get("license").getAsString());

            for (String key : new String[]{"main", "client"}) {
                var entrypoints = mod.getAsJsonObject("entrypoints").getAsJsonArray(key);
                assertEquals(1, entrypoints.size());
                String classPath = entrypoints.get(0).getAsString().replace('.', '/') + ".class";
                var entry = jar.getEntry(classPath);
                assertNotNull(entry, "Declared entrypoint must be packaged: " + key);
                try (var bytes = new DataInputStream(jar.getInputStream(entry))) {
                    assertEquals(0xCAFEBABE, bytes.readInt());
                    bytes.readUnsignedShort();
                    assertEquals(69, bytes.readUnsignedShort(), "Entrypoint must target Java 25");
                }
            }
            assertEquals(2, mod.getAsJsonArray("mixins").size(), "Common spawn and isolated client adapters");
            String config = mod.getAsJsonArray("mixins").get(0).getAsString();
            assertEquals("realsize.spawn.mixins.json", config);
            var clientConfig = mod.getAsJsonArray("mixins").get(1).getAsJsonObject();
            assertEquals("client", clientConfig.get("environment").getAsString());
            assertEquals("realsize.client.mixins.json", clientConfig.get("config").getAsString());
            try (var reader = new InputStreamReader(jar.getInputStream(jar.getEntry("realsize.client.mixins.json")),
                    StandardCharsets.UTF_8)) {
                var clientMixins = JsonParser.parseReader(reader).getAsJsonObject();
                assertTrue(clientMixins.get("required").getAsBoolean());
                assertFalse(clientMixins.has("mixins"), "No common client adapters");
                assertEquals(1, clientMixins.getAsJsonObject("injectors").get("defaultRequire").getAsInt());
                assertEquals(1, clientMixins.getAsJsonArray("client").size());
                assertEquals("EntityRenderDistanceMixin", clientMixins.getAsJsonArray("client").get(0).getAsString());
                assertNotNull(jar.getEntry(clientMixins.get("package").getAsString().replace('.', '/')
                        + "/EntityRenderDistanceMixin.class"));
            }
            JsonObject mixins;
            try (var reader = new InputStreamReader(jar.getInputStream(jar.getEntry(config)), StandardCharsets.UTF_8)) {
                mixins = JsonParser.parseReader(reader).getAsJsonObject();
            }
            assertTrue(mixins.get("required").getAsBoolean(), "Spawn integration must fail closed");
            assertEquals(1, mixins.getAsJsonObject("injectors").get("defaultRequire").getAsInt());
            assertFalse(mixins.has("client"));
            for (var adapter : mixins.getAsJsonArray("mixins")) {
                assertNotNull(jar.getEntry(mixins.get("package").getAsString().replace('.', '/') + "/"
                        + adapter.getAsString() + ".class"), "Every declared adapter must ship");
            }
            assertFalse(mixins.has("plugin"), "Early spawn checks need no bytecode plugin");
            assertEquals(3, mixins.getAsJsonArray("mixins").size(), "Spawn checks and baby-state adapter ship");
            assertFalse(mod.getAsJsonObject("entrypoints").has("fabric-gametest"));
            assertFalse(jar.stream().anyMatch(entry -> entry.getName().contains("ScaffoldGameTests")),
                    "Development tests must not ship in the production JAR");
            assertFalse(jar.stream().anyMatch(entry -> entry.getName().contains("ClientRenderGameTests")),
                    "Client development tests must not ship either");
            assertArrayEquals(Files.readAllBytes(project.resolve("branding/icon.png")),
                    jar.getInputStream(jar.getEntry(mod.get("icon").getAsString())).readAllBytes());
            assertArrayEquals(Files.readAllBytes(project.resolve("LICENSE")),
                    jar.getInputStream(jar.getEntry("LICENSE_realsize")).readAllBytes());
        }
    }
}
