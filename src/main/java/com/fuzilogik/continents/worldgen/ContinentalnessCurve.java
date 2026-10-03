package com.fuzilogik.continents.worldgen;

public final class ContinentalnessCurve {
   private static final double OFFSET_AT_MAX = -0.38;
   private static final double XZ_SCALE_FACTOR_AT_MAX = 0.35;
   private static final double RUG_AT_MAX = 0.05;
   private static final double K_AT_MAX = 0.38;
   private static final double RUG_FREQ = 1.5;
   private static final double UMBRAL = -0.9;

   private ContinentalnessCurve() {
   }

   public static double t(int continentalidad) {
      int clamped = Math.max(0, Math.min(100, continentalidad));
      return clamped / 100.0;
   }

   public static double offset(int continentalidad) {
      return lerp(t(continentalidad), 0.0, -0.38);
   }

   public static double xzScaleFactor(int continentalidad) {
      return lerp(t(continentalidad), 1.0, 0.35);
   }

   public static double rug(int continentalidad) {
      return lerp(t(continentalidad), 0.0, 0.05);
   }

   public static double rugFreq() {
      return 1.5;
   }

   public static double k(int continentalidad) {
      return lerp(t(continentalidad), 1.0, 0.38);
   }

   public static double umbral() {
      return -0.9;
   }

   private static double lerp(double delta, double start, double end) {
      return start + delta * (end - start);
   }
}
