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
                verifyGolemWalkTempo(client);
                verifyOverrideWalkTempo(client, true);
                verifyWalkTempo(client);
                System.out.println("REALSIZE CLIENT PASS: " + mode + ", synced quarter-scale bee, local toggle, "
                        + "invalid reload, boundary/view scale, enlarged/baby/nonliving, frustum rejection");
            } finally {
                Entity.setViewScale(previous);
            }
        });
        server.runOnServer(instance -> require(((LivingEntity) instance.overworld().getEntity(id)).getScale() == 0.25F,
                "Client local reload must not replace server sizing state"));
        verifySyncedGolemReload(context, server);
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

    private static void verifyGolemWalkTempo(Minecraft client) throws Exception {
        var update = LivingEntity.class.getDeclaredMethod("updateWalkAnimation", float.class);
        update.setAccessible(true);
        var golem = EntityTypes.IRON_GOLEM.create(client.level, EntitySpawnReason.COMMAND);
        golem.getAttribute(Attributes.SCALE).setBaseValue(1.2);
        assertWalkState(golem, 1.0f / golem.getScale(), update);
        System.out.println("REALSIZE ENLARGED GOLEM PASS: scale=" + golem.getScale()
                + ", tempo=" + 1.0f / golem.getScale());
        var random = new java.util.Random(654123);
        for (int sample = 0; sample < 32; sample++) {
            golem.getAttribute(Attributes.SCALE).setBaseValue(0.0625f + random.nextFloat() * (16 - 0.0625f));
            assertWalkState(golem, Math.min(1 / golem.getScale(), 6), update);
        }
        System.out.println("REALSIZE GOLEM SCALE SWEEP PASS: 32 deterministic arbitrary attribute scales");
    }

    private static void verifySyncedGolemReload(ClientGameTestContext context, TestServerContext server) throws Exception {
        var path = FabricLoader.getInstance().getConfigDir().resolve("realsize.json");
        byte[] original = Files.readAllBytes(path);
        var config = JsonParser.parseString(new String(original, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        int id = server.computeOnServer(instance -> {
            var player = instance.getPlayerList().getPlayers().getFirst();
            var golem = EntityTypes.IRON_GOLEM.create(player.level(), EntitySpawnReason.COMMAND);
            golem.setNoAi(true);
            golem.setNoGravity(true);
            golem.setPos(player.position().add(2, 1, 4));
            player.level().addFreshEntity(golem);
            return golem.getId();
        });
        try {
            for (double scale : new double[]{0.73, 1, 1.2, 1.37, 2.75}) {
                config.addProperty("cap", 4);
                config.getAsJsonObject("entityScales").addProperty("minecraft:iron_golem", scale);
                server.runOnServer(instance -> {
                    Files.writeString(path, config.toString());
                    require(instance.getCommands().getDispatcher().execute("realsize reload",
                            instance.createCommandSourceStack()) == 1, "Server config reload must succeed");
                    require(((LivingEntity) instance.overworld().getEntity(id)).getScale() == (float) scale,
                            "Config reload must resize the existing golem");
                });
                context.waitFor(client -> client.level.getEntity(id) instanceof LivingEntity golem
                        && golem.getScale() == (float) scale);
                context.runOnClient(client -> {
                    var golem = (net.minecraft.world.entity.animal.golem.IronGolem) client.level.getEntity(id);
                    var update = LivingEntity.class.getDeclaredMethod("updateWalkAnimation", float.class);
                    update.setAccessible(true);
                    assertWalkState(golem, 1 / golem.getScale(), update);
                    var renderer = (net.minecraft.client.renderer.entity.IronGolemRenderer)
                            client.getEntityRenderDispatcher().getRenderer(golem);
                    var state = renderer.createRenderState();
                    renderer.extractRenderState(golem, state, 0.5f);
                    var expected = new net.minecraft.world.entity.WalkAnimationState();
                    for (float distance : new float[]{0.04f, 0.3f, 0.04f, 0}) {
                        expected.update(Math.min(distance * 4, 1), 0.4f, 1 / golem.getScale());
                    }
                    require(Math.abs(state.walkAnimationPos - expected.position(0.5f)) < 1e-5,
                            "Golem renderer must consume the scale-corrected phase after config reload");
                    var root = net.minecraft.client.model.animal.golem.IronGolemModel.createBodyLayer().bakeRoot();
                    var model = new net.minecraft.client.model.animal.golem.IronGolemModel(root);
                    model.setupAnim(state);
                    float swing = 1.5f * net.minecraft.util.Mth.triangleWave(expected.position(0.5f), 13)
                            * expected.speed(0.5f);
                    require(Math.abs(root.getChild("right_leg").xRot + swing) < 1e-5
                            && Math.abs(root.getChild("left_leg").xRot - swing) < 1e-5,
                            "Actual golem model legs must use corrected phase without changing swing amplitude");
                    System.out.println("REALSIZE SYNCED GOLEM MODEL PASS: config scale=" + scale
                            + ", synced=" + golem.getScale() + ", phase=" + state.walkAnimationPos);
                });
            }
        } finally {
            server.runOnServer(instance -> {
                Files.write(path, original);
                require(instance.getCommands().getDispatcher().execute("realsize reload",
                        instance.createCommandSourceStack()) == 1, "Restore original server fixture config");
                var golem = instance.overworld().getEntity(id);
                if (golem != null) golem.discard();
            });
        }
    }

    private static void verifyOverrideWalkTempo(Minecraft client, boolean enabled) throws Exception {
        var update = LivingEntity.class.getDeclaredMethod("updateWalkAnimation", float.class);
        update.setAccessible(true);
        for (float scale : new float[]{0.26f, 1, 1.37f, 2.5f}) {
            var camel = EntityTypes.CAMEL.create(client.level, EntitySpawnReason.COMMAND);
            camel.getAttribute(Attributes.SCALE).setBaseValue(scale);
            float tempo = enabled ? Math.min(1 / scale, 6) : 1;
            assertWalkState(camel, tempo, update, 6, 1, 0.2f);
            camel.setPose(net.minecraft.world.entity.Pose.SITTING);
            assertWalkState(camel, tempo, update, 0, 1, 0.2f);
            camel.setPose(net.minecraft.world.entity.Pose.STANDING);
            camel.dashAnimationState.start(0);
            assertWalkState(camel, tempo, update, 0, 1, 0.2f);
            camel.dashAnimationState.stop();
            camel.setBaby(true);
            assertWalkState(camel, enabled ? Math.min(3 / scale, 6) : 3, update, 6, 1, 0.2f);

            var frog = EntityTypes.FROG.create(client.level, EntitySpawnReason.COMMAND);
            frog.getAttribute(Attributes.SCALE).setBaseValue(scale);
            assertWalkState(frog, tempo, update, 25, 1, 0.4f);
            frog.jumpAnimationState.start(0);
            assertWalkState(frog, tempo, update, 0, 1, 0.4f);

            var creaking = EntityTypes.CREAKING.create(client.level, EntitySpawnReason.COMMAND);
            creaking.getAttribute(Attributes.SCALE).setBaseValue(scale);
            assertWalkState(creaking, tempo, update, 25, 3, 0.4f);
        }
        System.out.println("REALSIZE OVERRIDE WALK PASS: enabled=" + enabled
                + ", camel/frog/creaking, small/neutral/large, sitting/dashing/jumping, baby camel");
    }

    private static void verifyWalkTempo(Minecraft client) throws Exception {
        var update = LivingEntity.class.getDeclaredMethod("updateWalkAnimation", float.class);
        update.setAccessible(true);
        var spider = EntityTypes.SPIDER.create(client.level, EntitySpawnReason.COMMAND);
        spider.getAttribute(Attributes.SCALE).setBaseValue(0.26);
        var vanilla = new net.minecraft.world.entity.WalkAnimationState();
        for (int tick = 0; tick < 20; tick++) {
            update.invoke(spider, 0.04f);
            vanilla.update(0.16f, 0.4f, 1.0f);
        }
        require(Math.abs(spider.walkAnimation.position() - vanilla.position() / spider.getScale()) < 1e-5,
                "Quarter-scale spider must advance walk rhythm by inverse attribute scale");
        require(Math.abs(spider.walkAnimation.speed() - vanilla.speed()) < 1e-6,
                "Walk rhythm must preserve vanilla swing amplitude and smoothing");
        System.out.println("REALSIZE WALK TEMPO PASS: scale=" + spider.getScale()
                + ", phase=" + spider.walkAnimation.position() + ", vanilla=" + vanilla.position());
        var path = FabricLoader.getInstance().getConfigDir().resolve("realsize.json");
        byte[] original = Files.readAllBytes(path);
        var config = JsonParser.parseString(new String(original, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        try {
            for (boolean enabled : new boolean[]{false, true}) {
                config.addProperty("clientWalkTempoFix", enabled);
                config.addProperty("clientRenderDistanceFix", !enabled);
                Files.writeString(path, config.toString());
                require(ClientCommands.getActiveDispatcher().execute("realsize-client reload",
                        (FabricClientCommandSource) client.player.connection.getSuggestionsProvider()) == 1,
                        "Walk option must reload through the registered local command");
                require(RealSizeClient.walkTempoFixEnabled() == enabled
                        && RealSizeClient.renderDistanceFixEnabled() != enabled,
                        "Client corrections must be independently configurable");
                verifyOverrideWalkTempo(client, enabled);
                for (boolean baby : new boolean[]{false, true}) {
                    for (float scale : new float[]{0.1f, 0.26f, 0.5f, 1.0f, 1.45f}) {
                        var chicken = EntityTypes.CHICKEN.create(client.level, EntitySpawnReason.COMMAND);
                        chicken.setBaby(baby);
                        chicken.getAttribute(Attributes.SCALE).setBaseValue(scale);
                        float tempo = baby ? 3 : 1;
                        if (enabled) tempo = Math.min(tempo / scale, 6);
                        assertWalkState(chicken, tempo, update);
                    }
                }
                double base = client.player.getAttribute(Attributes.SCALE).getBaseValue();
                try {
                    client.player.getAttribute(Attributes.SCALE).setBaseValue(0.26);
                    assertWalkState(client.player, 1, update);
                } finally {
                    client.player.getAttribute(Attributes.SCALE).setBaseValue(base);
                }
            }
            config.addProperty("clientWalkTempoFix", "false");
            Files.writeString(path, config.toString());
            require(ClientCommands.getActiveDispatcher().execute("realsize-client reload",
                    (FabricClientCommandSource) client.player.connection.getSuggestionsProvider()) == 0,
                    "Non-boolean walk option must fail");
            require(RealSizeClient.walkTempoFixEnabled() && !RealSizeClient.renderDistanceFixEnabled(),
                    "Invalid local option must retain both previous options");
        } finally {
            Files.write(path, original);
            RealSizeClient.reloadOptions();
        }
        System.out.println("REALSIZE WALK CASES PASS: independent toggles, baby/adult, cap, neutral/enlarged, player, invalid reload");
    }

    private static void assertWalkState(LivingEntity mob, float tempo, java.lang.reflect.Method update) throws Exception {
        assertWalkState(mob, tempo, update, 4, 1, 0.4f);
    }

    private static void assertWalkState(LivingEntity mob, float tempo, java.lang.reflect.Method update,
            float distanceGain, float targetCap, float smoothing) throws Exception {
        mob.walkAnimation.stop();
        var expected = new net.minecraft.world.entity.WalkAnimationState();
        for (float distance : new float[]{0.04f, 0.3f, 0.04f, 0.0f}) {
            update.invoke(mob, distance);
            expected.update(Math.min(distance * distanceGain, targetCap), smoothing, tempo);
            require(Math.abs(mob.walkAnimation.speed() - expected.speed()) < 1e-6,
                    "Animation amplitude/smoothing must remain vanilla");
            for (float partial : new float[]{0, 0.5f, 1}) {
                require(Math.abs(mob.walkAnimation.position(partial) - expected.position(partial)) < 1e-5,
                        "Walk tempo/partial interpolation mismatch: scale=" + mob.getScale() + ", tempo=" + tempo);
            }
        }
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
