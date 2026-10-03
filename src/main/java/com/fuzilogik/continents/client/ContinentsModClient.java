package com.fuzilogik.continents.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterPresetEditorsEvent;

@Mod(value = "continents", dist = Dist.CLIENT)
public final class ContinentsModClient {
    public ContinentsModClient(IEventBus modEventBus) {
        modEventBus.addListener(this::registerPresetEditors);
    }

    private void registerPresetEditors(RegisterPresetEditorsEvent event) {
        event.register(ContinentsPresetEditor.PRESET_KEY, ContinentsPresetEditor.INSTANCE);
    }
}
