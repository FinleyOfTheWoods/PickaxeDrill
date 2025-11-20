package uk.co.finleyofthewoods.pickaxedrill;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import uk.co.finleyofthewoods.pickaxedrill.events.DrillEventHandler;

public class Pickaxedrill implements ModInitializer {
    @Override
    public void onInitialize() {
        PlayerBlockBreakEvents.BEFORE.register(new DrillEventHandler());
    }
}
