package com.fuzilogik.continents;

import com.fuzilogik.continents.command.MapCommand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod("continents")
public class ContinentsMod {
   public static final String MOD_ID = "continents";
   public static final Logger LOGGER = LoggerFactory.getLogger("continents");

   public ContinentsMod(IEventBus modEventBus) {
      NeoForge.EVENT_BUS.addListener(this::registerCommands);
      LOGGER.info("Continents loaded: world preset \"continents:continents\" available.");
   }

   private void registerCommands(RegisterCommandsEvent event) {
      MapCommand.register(event.getDispatcher());
   }
}
