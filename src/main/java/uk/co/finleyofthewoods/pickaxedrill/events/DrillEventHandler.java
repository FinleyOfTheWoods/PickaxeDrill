package uk.co.finleyofthewoods.pickaxedrill.events;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.world.World;

import java.util.Set;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.finleyofthewoods.pickaxedrill.utils.DrillLogic;
import uk.co.finleyofthewoods.pickaxedrill.utils.DrillLogic.DrillDirection;
import uk.co.finleyofthewoods.pickaxedrill.utils.DrillLogic.DrillConfig;

public class DrillEventHandler implements PlayerBlockBreakEvents.Before {
    public static final Logger LOGGER = LoggerFactory.getLogger(DrillEventHandler.class);

    private static final Set<Block> DENY_LIST = Set.of(
            Blocks.BEDROCK,
            Blocks.BARRIER,
            Blocks.COMMAND_BLOCK,
            Blocks.END_PORTAL_FRAME,
            Blocks.END_PORTAL
    );

    @Override
    public boolean beforeBlockBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        ///  Check if the current position is already in the BREAKING_POSITIONS set,
        ///  or if the world is client-side.
        if (world.isClient()) {
            LOGGER.debug("Skipping block position: {}", pos);
            return true;
        }
        Set<BlockPos> BreakingPositions = new HashSet<>();
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
        Axis heightAxis = drillConfig.height();

        /// Handle Drill Width
        int widthEnchantmentLevel = 3;
        Axis widthAxis = drillConfig.width();

        /// Handle Drill Depth
        Axis depthAxis = drillConfig.depth();
        int directionSign = drillConfig.directionSign();
        int depthEnchantmentLevel = 3;

        LOGGER.debug("Drill Config: height={}, width={}, depth={}, directionSign={}", heightAxis, widthAxis, depthAxis, directionSign);

        for (int h = -heightEnchantmentLevel; h <= heightEnchantmentLevel; h++) {
            for (int w = -widthEnchantmentLevel; w <= widthEnchantmentLevel; w++) {
                for (int d = 0; d <= depthEnchantmentLevel; d++) {
                    int xOffset = 0;
                    int yOffset = 0;
                    int zOffset = 0;

                    /// Height Offset
                    if (heightAxis == Axis.X) xOffset += h;
                    else if (heightAxis == Axis.Y) yOffset += h;
                    else if (heightAxis == Axis.Z) zOffset += h;

                    /// Width Offset
                    if (widthAxis == Axis.X) xOffset += w;
                    else if (widthAxis == Axis.Y) yOffset += w;
                    else if (widthAxis == Axis.Z) zOffset += w;

                    /// Depth Offset
                    int depthOffset = d * directionSign;
                    if (depthAxis == Axis.X) xOffset += depthOffset;
                    else if (depthAxis == Axis.Y) yOffset += depthOffset;
                    else if (depthAxis == Axis.Z) zOffset += depthOffset;

                    /// Set position based on offsets and add to the list.
                    BlockPos newPosition = pos.add(xOffset, yOffset, zOffset);
                    BreakingPositions.add(newPosition);
                }
            }
        }
        breakBlocks(world, player, BreakingPositions, heldItemStack);
        return true;
    }

    private void breakBlocks(World world, PlayerEntity player, Set<BlockPos> blockPosSet, ItemStack heldItemStack) {
        for (BlockPos pos : blockPosSet) {
            BlockState state = world.getBlockState(pos);
            LOGGER.debug("Checking block at {}, {}", pos, state.getBlock().toString());

            /// Check Hardness (Bedrock is -1.0f)
            if (state.getHardness(world, pos) == -1.0f || DENY_LIST.contains(state.getBlock())) {
                LOGGER.debug("Skipping block position: {}. Block: {}", pos, state.getBlock().toString());
                continue;
            }

            /// Check Suitability
            /// - Checks if the block is meant for a pickaxe (filters Wood/Dirt)
            /// - Checks if the tool tier is high enough (e.g., Stone Pick vs. Diamond Ore)
            if (!state.isIn(BlockTags.PICKAXE_MINEABLE) || !heldItemStack.isSuitableFor(state)) {
                LOGGER.debug("Skipping block position: {}. Block: {}, Tool: {}", pos, state.getBlock().toString(), heldItemStack.getItem().toString());
                continue;
            }

            /// Check if the player can mine the block
            if (heldItemStack.canMine(state, world, pos, player)) {
                if (world.breakBlock(pos, true)) {
                    LOGGER.debug("Block at {} broken, Block: {}", pos, state.getBlock().toString());
                } else {
                    LOGGER.warn("Failed to break block at {}, Block: {}", pos, state.getBlock().toString());
                }
            }
        }
        /// Clear the list of positions to free up memory.
        blockPosSet.clear();
    }
}
