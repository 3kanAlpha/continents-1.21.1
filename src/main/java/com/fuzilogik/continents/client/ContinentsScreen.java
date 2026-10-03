package com.fuzilogik.continents.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext.DimensionsUpdater;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

public final class ContinentsScreen extends Screen {
   private static final Component TITLE = Component.translatable("continents.slider.title");
   private static final int WIDGET_WIDTH = 210;
   private final CreateWorldScreen parent;
   private final WorldCreationContext settings;
   private ContinentalnessSlider slider;

   ContinentsScreen(CreateWorldScreen parent, WorldCreationContext settings) {
      super(TITLE);
      this.parent = parent;
      this.settings = settings;
   }

   protected void init() {
      int centerX = this.width / 2;
      int sliderY = this.height / 2 - 20;
      this.slider = (ContinentalnessSlider)this.addRenderableWidget(
         new ContinentalnessSlider(centerX - 105, sliderY, 210, 20, currentContinentalidad(this.settings))
      );
      int buttonY = sliderY + 32;
      int halfWidth = 100;
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onDone()).bounds(centerX - 105, buttonY, halfWidth, 20).build());
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).bounds(centerX + 5, buttonY, halfWidth, 20).build());
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      super.render(graphics, mouseX, mouseY, partialTick);
      graphics.drawCenteredString(this.font, TITLE, this.width / 2, this.height / 2 - 44, 0xFFFFFF);
   }

   public void onClose() {
      this.minecraft.setScreen(this.parent);
   }

   private void onDone() {
      int continentalidad = this.slider.continentalidad();
      this.parent.getUiState().updateDimensions(continentalidadConfigurator(continentalidad));
      this.onClose();
   }

   private static int currentContinentalidad(WorldCreationContext settings) {
      return settings.selectedDimensions().overworld() instanceof NoiseBasedChunkGenerator noiseGenerator
         ? noiseGenerator.generatorSettings().unwrapKey().map(key -> parseContinentalidad(key.location())).orElse(100)
         : 100;
   }

   private static int parseContinentalidad(ResourceLocation settingsId) {
      if (settingsId.equals(NoiseGeneratorSettings.OVERWORLD.location())) {
         return 0;
      }

      if (!settingsId.getNamespace().equals("continents")) {
         return 100;
      }

      String prefix = "continentalness_";
      String path = settingsId.getPath();
      if (path.startsWith(prefix)) {
         try {
            return Integer.parseInt(path.substring(prefix.length()));
         } catch (NumberFormatException var4) {
         }
      }

      return 100;
   }

   static DimensionsUpdater continentalidadConfigurator(int continentalidad) {
      return (registryAccess, dimensions) -> {
         ChunkGenerator currentOverworld = dimensions.overworld();
         BiomeSource biomeSource = currentOverworld.getBiomeSource();
         Registry<NoiseGeneratorSettings> noiseSettingsRegistry = registryAccess.registryOrThrow(Registries.NOISE_SETTINGS);
         ResourceKey<NoiseGeneratorSettings> settingsKey = continentalidad == 0
            ? NoiseGeneratorSettings.OVERWORLD
            : ResourceKey.create(Registries.NOISE_SETTINGS, ResourceLocation.fromNamespaceAndPath("continents", "continentalness_" + continentalidad));
         Holder<NoiseGeneratorSettings> settingsHolder = noiseSettingsRegistry.getHolderOrThrow(settingsKey);
         ChunkGenerator generator = new NoiseBasedChunkGenerator(biomeSource, settingsHolder);
         return dimensions.replaceOverworldGenerator(registryAccess, generator);
      };
   }
}
