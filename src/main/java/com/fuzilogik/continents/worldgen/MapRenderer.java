package com.fuzilogik.continents.worldgen;

import java.awt.image.BufferedImage;
import java.util.stream.IntStream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise.NoiseParameters;

public final class MapRenderer {
   public static final int SIZE = 1024;
   public static final int CONTINENTALIDAD_MIN = 0;
   public static final int CONTINENTALIDAD_MAX = 100;
   private static final int STOP_STEP = 5;
   public static final int RADIO_MIN = 1000;
   public static final int RADIO_MAX = 30000;
   public static final int RADIO_DEFAULT = 10000;
   private static final int SAMPLE_Y = 63;
   private static final int ORIGIN_CROSS_HALF_LENGTH = 5;
   private static final int ORIGIN_COLOR = -16777216;

   private MapRenderer() {
   }

   public static int snapContinentalidad(int raw) {
      int clamped = Math.max(0, Math.min(100, raw));
      int snapped = Math.round(clamped / 5.0F) * 5;
      return Math.max(0, Math.min(100, snapped));
   }

   public static int clampRadio(int raw) {
      return Math.max(1000, Math.min(30000, raw));
   }

   public static double stepBlocksPerPixel(int radio) {
      return 2.0 * radio / 1024.0;
   }

   public static int blockCoordForPixel(int pixel, int radio) {
      double step = stepBlocksPerPixel(radio);
      return (int)Math.floor(-radio + (pixel + 0.5) * step);
   }

   public static int pixelForBlockCoord(int blockCoord, int radio) {
      double step = stepBlocksPerPixel(radio);
      int pixel = (int)Math.floor((blockCoord + radio) / step);
      return Math.max(0, Math.min(1023, pixel));
   }

   public static String fileName(long seed, int continentalidad, int radio) {
      return fileNameForLabel(seed, "c" + continentalidad, radio);
   }

   public static String fileNameForLabel(long seed, String label, int radio) {
      return "map_" + seed + "_" + label + "_r" + radio + ".png";
   }

   public static String labelForId(String namespace, String path) {
      return "s" + namespace + "_" + path.replace('/', '_');
   }

   public static BufferedImage render(
      ServerLevel overworld, Holder<NoiseGeneratorSettings> settingsHolder, HolderGetter<NoiseParameters> noiseLookup, long seed, int radio
   ) {
      RandomState randomState = RandomState.create((NoiseGeneratorSettings)settingsHolder.value(), noiseLookup, seed);
      Sampler sampler = randomState.sampler();
      BiomeSource biomeSource = overworld.getChunkSource().getGenerator().getBiomeSource();
      int quartY = QuartPos.fromBlock(63);
      BufferedImage image = new BufferedImage(1024, 1024, 2);
      IntStream.range(0, 1024).parallel().forEach(py -> {
         int blockZ = blockCoordForPixel(py, radio);
         int quartZ = QuartPos.fromBlock(blockZ);
         int[] row = new int[1024];

         for (int px = 0; px < 1024; px++) {
            int blockX = blockCoordForPixel(px, radio);
            int quartX = QuartPos.fromBlock(blockX);
            Holder<Biome> biome = biomeSource.getNoiseBiome(quartX, quartY, quartZ, sampler);
            row[px] = BiomeColors.colorFor(biome);
         }

         image.setRGB(0, py, 1024, 1, row, 0, 1024);
      });
      drawOriginCross(image, radio);
      return image;
   }

   private static void drawOriginCross(BufferedImage image, int radio) {
      int centerX = pixelForBlockCoord(0, radio);
      int centerY = pixelForBlockCoord(0, radio);

      for (int d = -5; d <= 5; d++) {
         setIfInBounds(image, centerX + d, centerY);
         setIfInBounds(image, centerX, centerY + d);
      }
   }

   private static void setIfInBounds(BufferedImage image, int x, int y) {
      if (x >= 0 && x < 1024 && y >= 0 && y < 1024) {
         image.setRGB(x, y, -16777216);
      }
   }
}
