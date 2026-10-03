package com.fuzilogik.continents.command;

import com.fuzilogik.continents.ContinentsMod;
import com.fuzilogik.continents.worldgen.MapRenderer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.synth.NormalNoise.NoiseParameters;

public final class MapCommand {
   private static final AtomicBoolean RENDERING = new AtomicBoolean(false);
   private static final String[] SUGGESTED_VALUES = new String[]{"0", "25", "50", "75", "100"};

   private MapCommand() {
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(Commands.literal("continents")
         .then(Commands.literal("map").requires(source -> source.hasPermission(2))
            .then(Commands.argument("continentalness", IntegerArgumentType.integer(0, 100))
               .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(SUGGESTED_VALUES), builder))
               .executes(ctx -> runStop(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "continentalness"), 10000))
               .then(Commands.argument("radius", IntegerArgumentType.integer(1000, 30000))
                  .executes(ctx -> runStop(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "continentalness"),
                     IntegerArgumentType.getInteger(ctx, "radius")))))
            .then(Commands.literal("settings")
               .then(Commands.argument("id", ResourceLocationArgument.id())
                  .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                     ctx.getSource().getServer().registryAccess().registryOrThrow(Registries.NOISE_SETTINGS).keySet(), builder))
                  .executes(ctx -> runSettings(ctx.getSource(), ResourceLocationArgument.getId(ctx, "id"), 10000))
                  .then(Commands.argument("radius", IntegerArgumentType.integer(1000, 30000))
                     .executes(ctx -> runSettings(ctx.getSource(), ResourceLocationArgument.getId(ctx, "id"),
                        IntegerArgumentType.getInteger(ctx, "radius"))))))));
   }

   private static int runStop(CommandSourceStack source, int continentalidadRaw, int radioRaw) {
      int continentalidad = MapRenderer.snapContinentalidad(continentalidadRaw);
      int radio = MapRenderer.clampRadio(radioRaw);
      MinecraftServer server = source.getServer();

      Holder<NoiseGeneratorSettings> settingsHolder;
      try {
         settingsHolder = resolveStopSettings(server, continentalidad);
      } catch (Exception e) {
         source.sendFailure(Component.translatable("continents.map.stop_missing", new Object[]{continentalidad}));
         return 0;
      }

      return startRender(source, server, settingsHolder, "c" + continentalidad, radio);
   }

   private static int runSettings(CommandSourceStack source, ResourceLocation id, int radioRaw) {
      int radio = MapRenderer.clampRadio(radioRaw);
      MinecraftServer server = source.getServer();
      Registry<NoiseGeneratorSettings> registry = server.registryAccess().registryOrThrow(Registries.NOISE_SETTINGS);
      Optional<? extends Holder<NoiseGeneratorSettings>> resolved = registry.getHolder(id);
      if (resolved.isEmpty()) {
         source.sendFailure(Component.translatable("continents.map.unknown_settings", new Object[]{id.toString()}));
         return 0;
      } else {
         String label = MapRenderer.labelForId(id.getNamespace(), id.getPath());
         return startRender(source, server, (Holder<NoiseGeneratorSettings>)resolved.get(), label, radio);
      }
   }

   private static int startRender(CommandSourceStack source, MinecraftServer server, Holder<NoiseGeneratorSettings> settingsHolder, String label, int radio) {
      if (!RENDERING.compareAndSet(false, true)) {
         source.sendSystemMessage(Component.translatable("continents.map.busy").withStyle(ChatFormatting.RED));
         return 0;
      } else {
         ServerLevel overworld = server.overworld();
         long seed = overworld.getSeed();
         source.sendSystemMessage(Component.translatable("continents.map.generating", new Object[]{label, radio}));
         Thread renderThread = new Thread(() -> renderInBackground(source, server, overworld, settingsHolder, label, radio, seed), "continents-map");
         renderThread.setDaemon(true);
         renderThread.start();
         return 1;
      }
   }

   private static void renderInBackground(
      CommandSourceStack source,
      MinecraftServer server,
      ServerLevel overworld,
      Holder<NoiseGeneratorSettings> settingsHolder,
      String label,
      int radio,
      long seed
   ) {
      long start = System.nanoTime();

      try {
         HolderGetter<NoiseParameters> noiseLookup = server.registryAccess().registryOrThrow(Registries.NOISE).asLookup();
         BufferedImage image = MapRenderer.render(overworld, settingsHolder, noiseLookup, seed, radio);
         Path outDir = server.getServerDirectory().resolve("continents-maps");
         Files.createDirectories(outDir);
         Path outFile = outDir.resolve(MapRenderer.fileNameForLabel(seed, label, radio));
         ImageIO.write(image, "png", outFile.toFile());
         double seconds = (System.nanoTime() - start) / 1.0E9;
         server.execute(() -> reportDone(source, outFile, seconds));
      } catch (Exception e) {
         ContinentsMod.LOGGER.error("/continents map (label={}, radio={}) falló", new Object[]{label, radio, e});
         server.execute(() -> source.sendSystemMessage(Component.translatable("continents.map.error").withStyle(ChatFormatting.RED)));
      } finally {
         RENDERING.set(false);
      }
   }

   private static void reportDone(CommandSourceStack source, Path outFile, double seconds) {
      Component message = Component.translatable("continents.map.done", new Object[]{String.format("%.1f", seconds), outFile.toString()})
         .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, outFile.toAbsolutePath().toString())));
      source.sendSuccess(() -> message, true);
   }

   private static Holder<NoiseGeneratorSettings> resolveStopSettings(MinecraftServer server, int continentalidad) {
      Registry<NoiseGeneratorSettings> registry = server.registryAccess().registryOrThrow(Registries.NOISE_SETTINGS);
      ResourceKey<NoiseGeneratorSettings> key = continentalidad == 0
         ? NoiseGeneratorSettings.OVERWORLD
         : ResourceKey.create(Registries.NOISE_SETTINGS, ResourceLocation.fromNamespaceAndPath("continents", "continentalness_" + continentalidad));
      return registry.getHolderOrThrow(key);
   }
}
