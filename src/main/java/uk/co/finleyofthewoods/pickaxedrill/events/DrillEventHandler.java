package uk.co.finleyofthewoods.pickaxedrill.events;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.world.World;

import java.util.*;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.finleyofthewoods.pickaxedrill.utils.DrillLogic;
import uk.co.finleyofthewoods.pickaxedrill.utils.DrillLogic.DrillDirection;
import uk.co.finleyofthewoods.pickaxedrill.utils.DrillLogic.DrillConfig;

public class DrillEventHandler implements PlayerBlockBreakEvents.Before {
    public static final Logger LOGGER = LoggerFactory.getLogger(DrillEventHandler.class);
    private static final List<BlockPos> BREAKING_POSITIONS = new ArrayList<>();

    @Override
    public boolean beforeBlockBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        ///  Check if the current position is already in the BREAKING_POSITIONS set,
        ///  or if the world is client-side.
        ;
        if (world.isClient() || BREAKING_POSITIONS.contains(pos)) {
            LOGGER.debug("Skipping block position: {}", pos);
            return true;
        }

        ///  Get the player's held item.
        ItemStack heldItemStack = player.getMainHandStack();

        ///  Check if the held item is a Pickaxe, Drill enchants do not apply to any other Item type,
        ///  or if the held item is empty.
        if (!heldItemStack.isIn(ItemTags.PICKAXES) || heldItemStack.isEmpty()) {
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
        int depthEnchantmentLevel = 3;

        LOGGER.warn("Drill Config: height={}, width={}, depth={}, directionSign={}", heightAxis, widthAxis, depthAxis, directionSign);

        for (int h = -heightEnchantmentLevel; h <= heightEnchantmentLevel; h++) {
            for (int w = -widthEnchantmentLevel; w <= widthEnchantmentLevel; w++) {
                for (int d = 0; d <= depthEnchantmentLevel; d++) {
                    int xOffset = 0;
                    int yOffset = 0;
                    int zOffset = 0;

                    // Height Offset
                    if ("X".equals(heightAxis)) xOffset += h;
                    else if ("Y".equals(heightAxis)) yOffset += h;
                    else if ("Z".equals(heightAxis)) zOffset += h;

                    // Width Offset
                    if ("X".equals(widthAxis)) xOffset += w;
                    else if ("Y".equals(widthAxis)) yOffset += w;
                    else if ("Z".equals(widthAxis)) zOffset += w;

                    // Depth Offset
                    int depthOffset = d * directionSign;
                    if ("X".equals(depthAxis)) xOffset += depthOffset;
                    else if ("Y".equals(depthAxis)) yOffset += depthOffset;
                    else if ("Z".equals(depthAxis)) zOffset += depthOffset;
                    BlockPos newPosition = pos.add(xOffset, yOffset, zOffset);
                    BREAKING_POSITIONS.add(newPosition);
                }
            }
        }
        breakBlocks(world, player, heldItemStack);
        return true;
    }

    private static final List<Block> DENY_LIST = List.of(
            Blocks.BEDROCK,
            Blocks.BARRIER,
            Blocks.COMMAND_BLOCK,
            Blocks.END_PORTAL_FRAME,
            Blocks.END_PORTAL
            );

    private void breakBlocks(World world, PlayerEntity player, ItemStack heldItemStack) {
        for (BlockPos pos : BREAKING_POSITIONS) {
            BlockState state = world.getBlockState(pos);

            /// Check Hardness (Bedrock is -1.0f)
            if (state.getHardness(world, pos) == -1.0f) {
                continue;
            }

            /// Check Disallow List
            if (DENY_LIST.contains(state.getBlock())) {
                continue;
            }

            /// Check Suitability
            /// - Checks if the block is meant for a pickaxe (filters Wood/Dirt)
            /// - Checks if the tool tier is high enough (e.g., Stone Pick vs. Diamond Ore)
            if (!state.isIn(BlockTags.PICKAXE_MINEABLE) || !heldItemStack.isSuitableFor(state)) {
                continue;
            }

            if (heldItemStack.canMine(state, world, pos, player) && state.getHardness(world, pos) != -1.0F) {
                if (world.breakBlock(pos, true)) {
                    LOGGER.debug("Block at {} broken, Block: {}", pos, state.getBlock().toString());
                } else {
                    LOGGER.debug("Failed to break block at {}, Block: {}", pos, state.getBlock().toString());
                }
            }
        }
        BREAKING_POSITIONS.clear();
    }
}
