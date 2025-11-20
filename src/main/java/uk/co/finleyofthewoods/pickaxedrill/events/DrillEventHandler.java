package uk.co.finleyofthewoods.pickaxedrill.events;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.finleyofthewoods.pickaxedrill.utils.DrillLogic;
import uk.co.finleyofthewoods.pickaxedrill.utils.DrillLogic.DrillDirection;
import uk.co.finleyofthewoods.pickaxedrill.utils.DrillLogic.DrillConfig;

public class DrillEventHandler implements PlayerBlockBreakEvents.Before {
    public static final Logger LOGGER = LoggerFactory.getLogger(DrillEventHandler.class);
    private static final Set<BlockPos> BREAKING_POSITIONS = new HashSet<>();

    @Override
    public boolean beforeBlockBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        ///  Check if the current position is already in the BREAKING_POSITIONS set,
        ///  or if the world is client-side.
        if (BREAKING_POSITIONS.contains(pos) || world.isClient()) {
            LOGGER.debug("Skipping block position: {}", pos);
            return true;
        }

        ///  Get the player's held item.
        ItemStack heldItemStack = player.getMainHandStack();

        ///  Check if the held item is a Pickaxe, Drill enchants do not apply to any other Item type,
        ///  or if the held item is empty.
        if (!heldItemStack.isIn(ItemTags.PICKAXES) || heldItemStack.isEmpty()) {
            LOGGER.debug("Skipping block position: {}. Incorrect item type: {}", pos, heldItemStack.getItem().toString());
            return true;
        }

        ///  Add the targeted block position to the set of positions to be broken
        LOGGER.debug("Added block position to set: {}", pos);
        BREAKING_POSITIONS.add(pos);

        ///  Get direction player is looking.
        Axis axis = player.getFacing().getAxis(); // x, y, or z
        float pitch = player.getPitch(); // -90 (UP) to 90 (DOWN)
        Direction direction = player.getHorizontalFacing(); // north, south, east, or west

        DrillDirection drillDirection = DrillLogic.getDrillDirection(direction, axis, pitch);
        DrillConfig drillConfig = DrillLogic.getDrillConfig(drillDirection);

        return true;
    }
}
