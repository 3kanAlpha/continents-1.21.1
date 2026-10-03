package com.fuzilogik.continents.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

public final class ContinentsPresetEditor implements PresetEditor {
   public static final ResourceLocation PRESET_ID = ResourceLocation.fromNamespaceAndPath("continents", "continents");
   public static final ResourceKey<WorldPreset> PRESET_KEY = ResourceKey.create(Registries.WORLD_PRESET, PRESET_ID);
   public static final ContinentsPresetEditor INSTANCE = new ContinentsPresetEditor();

   private ContinentsPresetEditor() {
   }

   public Screen createEditScreen(CreateWorldScreen parent, WorldCreationContext settings) {
      return new ContinentsScreen(parent, settings);
   }
}
