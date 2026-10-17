package xyz.pyrehaven.realsize;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import xyz.pyrehaven.realsize.client.RealSizeClient;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Properties;

public final class ClientRenderGameTests implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try {
            verifyModes(context);
        } catch (Exception exception) {
            throw new AssertionError("Client verification failed", exception);
        }
    }

    private static void verifyModes(ClientGameTestContext context) throws Exception {
        try (var singleplayer = context.worldBuilder().create()) {
            verifyConnection(context, singleplayer.getServer(), singleplayer.getConnection(), "integrated");
        }
        Properties properties = new Properties();
        properties.setProperty("server-ip", "127.0.0.1");
        // The test API connects using getPort(), which retains configured 0 rather than the bound port.
        try (var socket = new java.net.ServerSocket(0, 0, java.net.InetAddress.getLoopbackAddress())) {
            properties.setProperty("server-port", Integer.toString(socket.getLocalPort()));
        }
        properties.setProperty("online-mode", "false");
        try (var server = context.worldBuilder().createServer(properties);
                var connection = server.connect()) {
            verifyConnection(context, server, connection, "dedicated TCP");
        }
    }

    private static void verifyConnection(ClientGameTestContext context, TestServerContext server,
            TestServerConnection connection, String mode) throws Exception {
        connection.waitForChunksDownload();
        int id = server.computeOnServer(instance -> {
            var player = instance.getPlayerList().getPlayers().getFirst();
            var bee = EntityTypes.BEE.create(player.level(), EntitySpawnReason.COMMAND);
            bee.setNoAi(true);
            bee.setNoGravity(true);
            bee.setPos(player.position().add(0, 1, 4));
            player.level().addFreshEntity(bee);
            require(bee.getScale() == 0.25F, "Server owner must size actual tracked bee");
            return bee.getId();
        });
        context.waitFor(client -> client.level.getEntity(id) instanceof LivingEntity bee && bee.getScale() == 0.25F
                && Math.abs(bee.getBoundingBox().getXsize() - bee.getDimensions(bee.getPose()).width()) < 1.0E-6);
        context.runOnClient(client -> {
            var bee = (LivingEntity) client.level.getEntity(id);
            require(Arrays.stream(Entity.class.getDeclaredMethods())
                    .filter(method -> method.getName().contains("realsize$unscaledDistance")).count() == 1,
                    "Exactly one required handler must exist on transformed Entity");
            double previous = Entity.getViewScale();
            try {
                Entity.setViewScale(1.0);
                verifyToggle(client, bee);
                verifyDistanceCases(client);
                System.out.println("REALSIZE CLIENT PASS: " + mode + ", synced quarter-scale bee, local toggle, "
                        + "invalid reload, boundary/view scale, enlarged/baby/nonliving, frustum rejection");
            } finally {
                Entity.setViewScale(previous);
            }
        });
        server.runOnServer(instance -> require(((LivingEntity) instance.overworld().getEntity(id)).getScale() == 0.25F,
                "Client local reload must not replace server sizing state"));
        server.runCommand("tp @a 256 -60 0");
        context.waitFor(client -> client.player.getX() > 250 && client.level.getEntity(id) == null);
        server.runOnServer(instance -> require(instance.overworld().getEntity(id) != null,
                "Distant removal must be vanilla client tracking, not server entity deletion"));
        System.out.println("REALSIZE CLIENT TRACKING PASS: " + mode + ", enabled correction cannot retain untracked bee");
    }

    private static void verifyToggle(Minecraft client, LivingEntity bee) throws Exception {
        System.out.println("REALSIZE CLIENT MEASURED: synced scale=" + bee.getScale()
                + ", viewScale=" + Entity.getViewScale() + ", vanilla bbox range="
                + bee.getBoundingBox().getSize() * 64.0 + ", corrected bbox range="
                + bee.getBoundingBox().getSize() / bee.getScale() * 64.0);
        var path = FabricLoader.getInstance().getConfigDir().resolve("realsize.json");
        byte[] original = Files.readAllBytes(path);
        var config = JsonParser.parseString(new String(original, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        try {
            // Deliberately disagree with the live server rule: rendering must use the synced entity scale.
            config.getAsJsonObject("entityScales").addProperty("minecraft:bee", 1.0);
            for (boolean enabled : new boolean[]{false, true, false, true}) {
                config.addProperty("clientRenderDistanceFix", enabled);
                Files.writeString(path, config.toString());
                require(ClientCommands.getActiveDispatcher().execute("realsize-client reload",
                        (FabricClientCommandSource) client.player.connection.getSuggestionsProvider()) == 1,
                        "Client command must reload valid config");
                require(RealSizeClient.renderDistanceFixEnabled() == enabled, "Local toggle must follow local file");
                require(bee.shouldRenderAtSqrDistance(900.0) == enabled, "Tracked bee distance must follow toggle");
                require(bee.shouldRenderAtSqrDistance(1.0), "Toggle must retain successful vanilla results");
            }
            Files.writeString(path, "{");
            require(ClientCommands.getActiveDispatcher().execute("realsize-client reload",
                    (FabricClientCommandSource) client.player.connection.getSuggestionsProvider()) == 0,
                    "Invalid client reload must fail");
            require(RealSizeClient.renderDistanceFixEnabled() && bee.shouldRenderAtSqrDistance(900.0),
                    "Invalid reload must retain previous active option");
        } finally {
            Files.write(path, original);
            RealSizeClient.reloadOptions();
        }
    }

    private static void verifyDistanceCases(Minecraft client) {
        for (float scale : new float[]{0.25F, 1.0F, 1.45F, 2.0F}) {
            var bee = EntityTypes.BEE.create(client.level, EntitySpawnReason.COMMAND);
            bee.getAttribute(Attributes.SCALE).setBaseValue(scale);
            bee.refreshDimensions();
            assertRange(bee, Math.max(bee.getBoundingBox().getSize(), bee.getBoundingBox().getSize() / scale));
        }
        var baby = EntityTypes.CHICKEN.create(client.level, EntitySpawnReason.COMMAND);
        baby.setBaby(true);
        baby.getAttribute(Attributes.SCALE).setBaseValue(0.25);
        baby.refreshDimensions();
        assertRange(baby, baby.getBoundingBox().getSize() / baby.getScale());
        var boat = EntityTypes.OAK_BOAT.create(client.level, EntitySpawnReason.COMMAND);
        assertRange(boat, boat.getBoundingBox().getSize());

        var bee = EntityTypes.BEE.create(client.level, EntitySpawnReason.COMMAND);
        bee.getAttribute(Attributes.SCALE).setBaseValue(0.25);
        bee.refreshDimensions();
        Frustum hidden = new Frustum(new Matrix4f(), new Matrix4f()) {
            @Override
            public boolean isVisible(AABB bounds) {
                return false;
            }
        };
        require(bee.shouldRender(bee.getX() + 30.0, bee.getY(), bee.getZ()), "Distance gate must allow bee");
        require(!client.getEntityRenderDispatcher().shouldRender(bee, hidden,
                bee.getX() + 30.0, bee.getY(), bee.getZ(), 1.0F), "Distance correction must not bypass frustum rejection");
    }

    private static void assertRange(Entity entity, double size) {
        for (double viewScale : new double[]{0.5, 1.0, 2.0}) {
            Entity.setViewScale(viewScale);
            double range = size * 64.0 * viewScale;
            double squared = range * range;
            require(entity.shouldRenderAtSqrDistance(Math.nextDown(squared)), "Inside strict range must render");
            require(!entity.shouldRenderAtSqrDistance(squared), "Exact strict range must not render");
            require(!entity.shouldRenderAtSqrDistance(Math.nextUp(squared)), "Beyond strict range must not render");
        }
        Entity.setViewScale(1.0);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
