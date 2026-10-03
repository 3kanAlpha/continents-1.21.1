package com.fuzilogik.continents.client;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

final class ContinentalnessSlider extends AbstractSliderButton {
   private static final int STEP = 5;
   private static final int MAX = 100;
   private static final int STEPS = 20;
   private int continentalidad;

   ContinentalnessSlider(int x, int y, int width, int height, int initial) {
      super(x, y, width, height, Component.empty(), snappedFraction(initial));
      this.continentalidad = snap(initial);
      this.updateMessage();
   }

   private static int snap(int value) {
      int clamped = Math.max(0, Math.min(100, value));
      return Math.round(clamped / 5.0F) * 5;
   }

   private static double snappedFraction(int value) {
      return snap(value) / 100.0;
   }

   int continentalidad() {
      return this.continentalidad;
   }

   protected void updateMessage() {
      this.setMessage(labelFor(this.continentalidad));
   }

   static Component labelFor(int continentalidad) {
      if (continentalidad == 0) {
         return Component.translatable("continents.slider.current");
      } else {
         return continentalidad == 100
            ? Component.translatable("continents.slider.classic")
            : Component.translatable("continents.slider.intermediate", new Object[]{continentalidad});
      }
   }

   protected void applyValue() {
      int stepIndex = Math.round((float)(this.value * 20.0));
      stepIndex = Math.max(0, Math.min(20, stepIndex));
      this.continentalidad = stepIndex * 5;
      this.value = this.continentalidad / 100.0;
   }
}
