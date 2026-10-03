package com.fuzilogik.continents.gametest;

import com.fuzilogik.continents.ContinentsMod;
import com.fuzilogik.continents.client.ContinentsPresetEditor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in development check; excluded from the release JAR. */
@EventBusSubscriber(modid = "continents", value = Dist.CLIENT)
public final class ContinentsClientSmokeTest {
    private static int stage;
    private static int ticks;
    private static int editorTicks;
    private static int worldTicks;
    private static CreateWorldScreen parent;
    private static Screen editor;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("continents.clientSmokeTest") || stage < 0) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            if (++ticks > 6000) throw new IllegalStateException("Client smoke test timed out at stage " + stage);
            if (mc.getOverlay() != null) return;
            if (stage == 0 && mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen onboarding) {
                onboarding.onClose();
                return;
            }
            if (stage == 0 && mc.screen instanceof TitleScreen) {
                Files.writeString(mc.gameDirectory.toPath().resolve("client-smoke-result.txt"), "RUNNING");
                stage = 1;
                CreateWorldScreen.openFresh(mc, mc.screen);
            } else if (stage == 1 && mc.screen instanceof CreateWorldScreen screen) {
                parent = screen;
                var ui = parent.getUiState();
                var preset = ui.getNormalPresetList().stream().filter(entry -> entry.preset() != null
                        && entry.preset().is(ContinentsPresetEditor.PRESET_KEY)).findFirst().orElseThrow();
                ui.setWorldType(preset);
                require(ui.getPresetEditor() == ContinentsPresetEditor.INSTANCE, "Preset editor registration");
                require(setting().equals("continents:continentalness_100"), "Default setting 100");
                editor = openEditor(mc);
                require(value() == 100, "Default slider value");
                stage = 2;
            } else if (stage == 2 && mc.screen == editor) {
                if (++editorTicks < 10) return;
                Screenshot.grab(mc.gameDirectory, "continents-customize.png", mc.getMainRenderTarget(), message -> {});
                var original = parent.getUiState().getSettings().selectedDimensions();
                var biomeSource = original.overworld().getBiomeSource();
                setValue(35);
                editor.onClose();
                require(setting().equals("continents:continentalness_100"), "Cancel must preserve setting");
                editor = openEditor(mc);
                require(value() == 100, "Reopen after cancel");
                setValue(50);
                invoke(editor, "onDone");
                require(setting().equals("continents:continentalness_50"), "Apply setting 50");
                editor = openEditor(mc);
                require(value() == 50, "Reopen after apply");
                setValue(0);
                invoke(editor, "onDone");
                require(setting().equals("minecraft:overworld"), "Apply vanilla setting");
                editor = openEditor(mc);
                require(value() == 0, "Reopen at zero");
                setValue(100);
                invoke(editor, "onDone");
                var updated = parent.getUiState().getSettings().selectedDimensions();
                require(updated.overworld().getBiomeSource() == biomeSource, "Preserve biome source");
                require(updated.dimensions().get(net.minecraft.world.level.dimension.LevelStem.NETHER)
                        == original.dimensions().get(net.minecraft.world.level.dimension.LevelStem.NETHER), "Preserve Nether");
                require(updated.dimensions().get(net.minecraft.world.level.dimension.LevelStem.END)
                        == original.dimensions().get(net.minecraft.world.level.dimension.LevelStem.END), "Preserve End");
                parent.getUiState().setName("Continents Smoke Test " + System.currentTimeMillis());
                parent.getUiState().setSeed("123456789");
                stage = 3;
                invoke(parent, "onCreate");
            } else if (stage == 3 && mc.screen instanceof ConfirmScreen confirm) {
                confirm.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                        .findFirst().orElseThrow().onPress();
            } else if (stage == 3 && mc.level != null && mc.player != null && mc.screen == null && mc.getSingleplayerServer() != null) {
                if (++worldTicks < 20) return;
                var generator = (NoiseBasedChunkGenerator) mc.getSingleplayerServer().overworld().getChunkSource().getGenerator();
                require(generator.generatorSettings().unwrapKey().orElseThrow().location().toString()
                        .equals("continents:continentalness_100"), "Created world setting");
                Screenshot.grab(mc.gameDirectory, "continents-world.png", mc.getMainRenderTarget(), message -> {});
                Files.writeString(mc.gameDirectory.toPath().resolve("client-smoke-result.txt"),
                        "PASS: preset listing, editor registration, default 100, cancel, apply 50/0/100, reopen, dimensions, world creation.");
                ContinentsMod.LOGGER.info("Continents client smoke test passed");
                stage = -1;
                mc.stop();
            }
        } catch (Exception e) {
            ContinentsMod.LOGGER.error("Continents client smoke test failed at stage " + stage, e);
            try { Files.writeString(mc.gameDirectory.toPath().resolve("client-smoke-result.txt"), "FAIL: " + e); }
            catch (java.io.IOException ignored) {}
            stage = -1;
            mc.stop();
        }
    }

    private static Screen openEditor(Minecraft mc) {
        Screen screen = parent.getUiState().getPresetEditor().createEditScreen(parent, parent.getUiState().getSettings());
        mc.setScreen(screen);
        return screen;
    }

    private static Object slider() throws Exception {
        Field field = editor.getClass().getDeclaredField("slider");
        field.setAccessible(true);
        return field.get(editor);
    }

    private static int value() throws Exception {
        return (int) invoke(slider(), "continentalidad");
    }

    private static void setValue(int value) throws Exception {
        Object slider = slider();
        Field field = AbstractSliderButton.class.getDeclaredField("value");
        field.setAccessible(true);
        field.setDouble(slider, value / 100.0);
        invoke(slider, "applyValue");
    }

    private static Object invoke(Object target, String name) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private static String setting() {
        var generator = (NoiseBasedChunkGenerator) parent.getUiState().getSettings().selectedDimensions().overworld();
        return generator.generatorSettings().unwrapKey().orElseThrow().location().toString();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
