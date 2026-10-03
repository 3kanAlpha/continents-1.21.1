package com.fuzilogik.continents.gametest;

import com.fuzilogik.continents.worldgen.ContinentalnessCurve;
import com.fuzilogik.continents.worldgen.MapRenderer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("continents")
@PrefixGameTestTemplate(false)
public final class ContinentsGameTests {
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void allSettingsGenerateTerrain(GameTestHelper helper) {
        var level = helper.getLevel();
        var registry = level.registryAccess().registryOrThrow(Registries.NOISE_SETTINGS);
        var noise = level.registryAccess().registryOrThrow(Registries.NOISE).asLookup();
        var source = level.registryAccess().registryOrThrow(Registries.WORLD_PRESET)
                .getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.NORMAL).value()
                .createWorldDimensions().overworld().getBiomeSource();
        for (int c = 0; c <= 100; c += 5) {
            var key = c == 0 ? NoiseGeneratorSettings.OVERWORLD : ResourceKey.create(Registries.NOISE_SETTINGS,
                    ResourceLocation.fromNamespaceAndPath("continents", "continentalness_" + c));
            var settings = registry.getHolderOrThrow(key);
            helper.assertTrue(settings.value().seaLevel() == 63, "Sea level at " + c);
            var state = RandomState.create(settings.value(), noise, 123456789L);
            var generator = new NoiseBasedChunkGenerator(source, settings);
            for (int x : new int[]{0, 512}) {
                var biome = source.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(63),
                        QuartPos.fromBlock(-x), state.sampler());
                helper.assertTrue(biome.unwrapKey().isPresent(), "Biome at " + c);
                int height = generator.getBaseHeight(x, -x, Heightmap.Types.WORLD_SURFACE_WG, level, state);
                helper.assertTrue(height >= -64 && height <= 320, "Terrain height at " + c + ": " + height);
                double continents = state.router().continents().compute(
                        new net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext(x, 63, -x));
                helper.assertTrue(Double.isFinite(continents), "Continentalness at " + c);
            }
            if (c > 0) {
                String prefix = "/data/continents/worldgen/density_function/continentalness_" + c + "/";
                var raw = json(prefix + "crudo.json").getAsJsonObject("argument");
                var mainAndDetail = raw.getAsJsonObject("argument2");
                near(helper, raw.get("argument1").getAsDouble(), ContinentalnessCurve.offset(c));
                near(helper, mainAndDetail.getAsJsonObject("argument1").get("xz_scale").getAsDouble(),
                        0.25 * ContinentalnessCurve.xzScaleFactor(c));
                near(helper, mainAndDetail.getAsJsonObject("argument2").get("argument1").getAsDouble(), ContinentalnessCurve.rug(c));
                near(helper, mainAndDetail.getAsJsonObject("argument2").getAsJsonObject("argument2").get("xz_scale").getAsDouble(),
                        ContinentalnessCurve.rugFreq());
                var tail = json(prefix + "continents.json");
                near(helper, tail.get("min_inclusive").getAsDouble(), -10.0);
                near(helper, tail.get("max_exclusive").getAsDouble(), ContinentalnessCurve.umbral());
                near(helper, tail.getAsJsonObject("when_in_range").get("argument1").getAsDouble(),
                        ContinentalnessCurve.umbral() * (1 - ContinentalnessCurve.k(c)));
                near(helper, tail.getAsJsonObject("when_in_range").getAsJsonObject("argument2").get("argument2").getAsDouble(),
                        ContinentalnessCurve.k(c));
            }
        }
        helper.assertTrue(MapRenderer.snapContinentalidad(2) == 0 && MapRenderer.snapContinentalidad(3) == 5
                && MapRenderer.snapContinentalidad(99) == 100, "Slider rounding");
        // Exercise the real command dispatcher and asynchronous PNG writer on the server.
        var server = level.getServer();
        var originalGenerator = server.overworld().getChunkSource().getGenerator();
        var commandSource = server.createCommandSourceStack();
        var dispatcher = server.getCommands().getDispatcher();
        var output = server.getServerDirectory().resolve("continents-maps");
        long seed = server.overworld().getSeed();
        String[] labels = {"c0", "scontinents_continentalness_100"};
        String[] commands = {"continents map 0 1000", "continents map settings continents:continentalness_100 1000"};
        try {
            helper.assertTrue(dispatcher.execute("continents map settings continents:missing", commandSource) == 0,
                    "Unknown settings must not start rendering");
            for (String label : labels) {
                java.nio.file.Files.deleteIfExists(output.resolve(MapRenderer.fileNameForLabel(seed, label, 1000)));
            }
            helper.assertTrue(dispatcher.execute(commands[0], commandSource) == 1, "Start vanilla map");
            helper.assertTrue(dispatcher.execute(commands[1], commandSource) == 0, "Reject concurrent map");
        } catch (Exception e) {
            throw new IllegalStateException("Map command setup", e);
        }
        awaitPng(helper, output.resolve(MapRenderer.fileNameForLabel(seed, labels[0], 1000)));
        try {
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
            while (dispatcher.execute(commands[1], commandSource) == 0) {
                helper.assertTrue(System.nanoTime() < deadline, "Map renderer remained busy");
                Thread.sleep(10);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Custom map command", e);
        }
        awaitPng(helper, output.resolve(MapRenderer.fileNameForLabel(seed, labels[1], 1000)));
        helper.assertTrue(server.overworld().getChunkSource().getGenerator() == originalGenerator,
                "Map rendering must not replace the Overworld generator");
        helper.succeed();
    }

    private static void awaitPng(GameTestHelper helper, java.nio.file.Path file) {
        // The GameTest server runs ticks without waiting; use a wall-clock deadline for the background writer.
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            try {
                if (java.nio.file.Files.exists(file)) {
                    var image = javax.imageio.ImageIO.read(file.toFile());
                    if (image != null) {
                        helper.assertTrue(image.getWidth() == 1024 && image.getHeight() == 1024, "Map dimensions");
                        return;
                    }
                }
            } catch (java.io.IOException e) {
                // ImageIO may observe a partially written file.
            }
            try { Thread.sleep(10); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
        }
        helper.assertTrue(false, "Map output timed out: " + file);
    }

    private static JsonObject json(String path) {
        try (var stream = ContinentsGameTests.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("Missing resource: " + path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(path, e);
        }
    }

    private static void near(GameTestHelper helper, double actual, double expected) {
        helper.assertTrue(Math.abs(actual - expected) < 1e-12, "Coefficient " + actual + " != " + expected);
    }
}
