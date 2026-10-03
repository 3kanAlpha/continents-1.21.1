package com.fuzilogik.continents.worldgen;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

public final class BiomeColors {
   private static final int FALLBACK = argb(127, 127, 127);
   private static final Map<ResourceKey<Biome>, Integer> COLORS = new HashMap<>();

   private BiomeColors() {
   }

   public static int colorFor(Holder<Biome> biome) {
      return biome.unwrapKey().map(key -> COLORS.getOrDefault(key, FALLBACK)).orElse(FALLBACK);
   }

   private static void put(ResourceKey<Biome> key, int rgb) {
      COLORS.put(key, 0xFF000000 | rgb);
   }

   private static int argb(int r, int g, int b) {
      return 0xFF000000 | r << 16 | g << 8 | b;
   }

   static {
      put(Biomes.DEEP_OCEAN, 662080);
      put(Biomes.DEEP_COLD_OCEAN, 1057605);
      put(Biomes.DEEP_LUKEWARM_OCEAN, 798290);
      put(Biomes.DEEP_FROZEN_OCEAN, 1783120);
      put(Biomes.OCEAN, 1925032);
      put(Biomes.COLD_OCEAN, 2781104);
      put(Biomes.LUKEWARM_OCEAN, 1998776);
      put(Biomes.WARM_OCEAN, 1746628);
      put(Biomes.FROZEN_OCEAN, 9421782);
      put(Biomes.RIVER, 5023456);
      put(Biomes.FROZEN_RIVER, 11065062);
      put(Biomes.BEACH, 15258250);
      put(Biomes.SNOWY_BEACH, 15724512);
      put(Biomes.STONY_SHORE, 9211002);
      put(Biomes.PLAINS, 9286496);
      put(Biomes.SUNFLOWER_PLAINS, 10929743);
      put(Biomes.FOREST, 5016123);
      put(Biomes.FLOWER_FOREST, 6270522);
      put(Biomes.BIRCH_FOREST, 9219658);
      put(Biomes.DARK_FOREST, 3034654);
      put(Biomes.OLD_GROWTH_BIRCH_FOREST, 8102720);
      put(Biomes.TAIGA, 3042906);
      put(Biomes.SNOWY_TAIGA, 7315608);
      put(Biomes.OLD_GROWTH_PINE_TAIGA, 2447946);
      put(Biomes.OLD_GROWTH_SPRUCE_TAIGA, 2052680);
      put(Biomes.SAVANNA, 11048015);
      put(Biomes.SAVANNA_PLATEAU, 9666879);
      put(Biomes.WINDSWEPT_SAVANNA, 10520148);
      put(Biomes.WINDSWEPT_HILLS, 7240542);
      put(Biomes.WINDSWEPT_GRAVELLY_HILLS, 8224110);
      put(Biomes.WINDSWEPT_FOREST, 5927760);
      put(Biomes.JUNGLE, 3120687);
      put(Biomes.SPARSE_JUNGLE, 4892734);
      put(Biomes.BAMBOO_JUNGLE, 6074931);
      put(Biomes.BADLANDS, 13201450);
      put(Biomes.ERODED_BADLANDS, 12081710);
      put(Biomes.WOODED_BADLANDS, 11037246);
      put(Biomes.MEADOW, 9423708);
      put(Biomes.CHERRY_GROVE, 15181508);
      put(Biomes.GROVE, 7315084);
      put(Biomes.SNOWY_SLOPES, 14212830);
      put(Biomes.FROZEN_PEAKS, 15266549);
      put(Biomes.JAGGED_PEAKS, 14542312);
      put(Biomes.STONY_PEAKS, 9211020);
      put(Biomes.SWAMP, 4942922);
      put(Biomes.MANGROVE_SWAMP, 3104334);
      put(Biomes.DESERT, 14730350);
      put(Biomes.SNOWY_PLAINS, 15265004);
      put(Biomes.ICE_SPIKES, 12117228);
      put(Biomes.MUSHROOM_FIELDS, 11565742);
      put(Biomes.DRIPSTONE_CAVES, 9072210);
      put(Biomes.LUSH_CAVES, 4098638);
      put(Biomes.DEEP_DARK, 1710626);
      put(Biomes.THE_VOID, 0);
   }
}
