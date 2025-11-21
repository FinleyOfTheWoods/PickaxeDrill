package uk.co.finleyofthewoods.pickaxedrill;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.finleyofthewoods.pickaxedrill.config.DrillConfig;
import uk.co.finleyofthewoods.pickaxedrill.enchantment.ModEnchantments;
import uk.co.finleyofthewoods.pickaxedrill.events.DrillEventHandler;

public class Pickaxedrill implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(Pickaxedrill.class);
    @Override
    public void onInitialize() {

        /// Load Config
        DrillConfig.load();

        PlayerBlockBreakEvents.BEFORE.register(new DrillEventHandler());
        ServerTickEvents.END_SERVER_TICK.register(DrillEventHandler::onTick);

        ModEnchantments.register();

        LOGGER.info("Pickaxe Drill initialized.");
    }
}
