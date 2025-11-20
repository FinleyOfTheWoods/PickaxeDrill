package uk.co.finleyofthewoods.pickaxedrill.events;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
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
        if (!heldItemStack.isIn(ItemTags.PICKAXES) || heldItemStack.isEmpty() || !EnchantmentHelper.hasEnchantments(heldItemStack)) {
            LOGGER.debug("Skipping block position: {}. Incorrect item type: {} or has no enchantments.", pos, heldItemStack.getItem().toString());
            return true;
        }

        ///  Get direction player is looking.
        Axis axis = player.getFacing().getAxis(); // x, y, or z
        float pitch = player.getPitch(); // -90 (UP) to 90 (DOWN)
        Direction direction = player.getHorizontalFacing(); // north, south, east, or west

        DrillDirection drillDirection = DrillLogic.getDrillDirection(direction, axis, pitch);
        DrillConfig drillConfig = DrillLogic.getDrillConfig(drillDirection);

        /// RegistryEntry<Enchantment> enchantmentRegistryEntry = (RegistryEntry<Enchantment>) Enchantments.EFFICIENCY;
        /// int enchantmentLevel = EnchantmentHelper.getLevel(enchantmentRegistryEntry, heldItemStack);

        /// Handle Drill Height
        int heightEnchantmentLevel = 3;
        String heightAxis = drillConfig.height();

        /// Handle Drill Width
        int widthEnchantmentLevel = 3;
        String widthAxis = drillConfig.width();

        /// Handle Drill Depth
        String depthAxis = drillConfig.depth();
        int directionSign = drillConfig.directionSign();
        int depthEnchantmentLevel = 3 * directionSign;

        for (int h = -heightEnchantmentLevel; h <= heightEnchantmentLevel; h++) {
            for (int w = -widthEnchantmentLevel; w <= widthEnchantmentLevel; w++) {
                for (int d = 0; d <= depthEnchantmentLevel; d++) {
                    int xOffset = 0;
                    int yOffset = 0;
                    int zOffset = 0;

                    // Height Offset
                    if ("x".equals(heightAxis)) xOffset += h;
                    else if ("y".equals(heightAxis)) yOffset += h;
                    else if ("z".equals(heightAxis)) zOffset += h;

                    // Width Offset
                    if ("x".equals(widthAxis)) xOffset += w;
                    else if ("y".equals(widthAxis)) yOffset += w;
                    else if ("z".equals(widthAxis)) zOffset += w;

                    // Depth Offset
                    int depthOffset = d * directionSign;
                    if ("x".equals(depthAxis)) xOffset += depthOffset;
                    else if ("y".equals(depthAxis)) yOffset += depthOffset;
                    else if ("z".equals(depthAxis)) zOffset += depthOffset;

                    BREAKING_POSITIONS.add(pos.add(xOffset, yOffset, zOffset));
                }
            }
        }
        breakBlocks(world, player, heldItemStack);
        return true;
    }

    private void breakBlocks(World world, PlayerEntity player, ItemStack heldItemStack) {
        player.sendMessage(Text.literal("breaking " + BREAKING_POSITIONS.size() + " blocks"), false);
        for (BlockPos pos : BREAKING_POSITIONS) {
            BlockState state = world.getBlockState(pos);
            if (heldItemStack.canMine(state, world, pos, player)) {
                world.breakBlock(pos, true);
            }
            BREAKING_POSITIONS.remove(pos);
        }
    }
}
