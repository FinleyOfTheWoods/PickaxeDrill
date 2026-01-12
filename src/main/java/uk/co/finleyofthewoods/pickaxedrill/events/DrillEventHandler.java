package uk.co.finleyofthewoods.pickaxedrill.events;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.world.World;

import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.finleyofthewoods.pickaxedrill.config.DrillConfig;
import uk.co.finleyofthewoods.pickaxedrill.enchantment.ModEnchantments;
import uk.co.finleyofthewoods.pickaxedrill.utils.DrillLogic;
import uk.co.finleyofthewoods.pickaxedrill.utils.DrillLogic.DrillDirection;
import uk.co.finleyofthewoods.pickaxedrill.utils.DrillLogic.DrillParams;
import uk.co.finleyofthewoods.pickaxedrill.utils.LedgerLogger;

public class DrillEventHandler implements PlayerBlockBreakEvents.Before {
    public static final Logger LOGGER = LoggerFactory.getLogger(DrillEventHandler.class);

    // Queue to hold active drill tasks
    private static final Set<DrillTask> TASKS = new HashSet<>();

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
        if (drillDirection == null) return false;
        DrillParams drillConfig = DrillLogic.getDrillConfig(drillDirection);

        int enchantmentLevel = getDrillLevel(world, heldItemStack);

        /// Handle Drill Height
        Axis heightAxis = drillConfig.height();

        /// Handle Drill Width
        Axis widthAxis = drillConfig.width();

        /// Handle Drill Depth
        Axis depthAxis = drillConfig.depth();
        int directionSign = drillConfig.directionSign();

        LOGGER.debug("Drill Config: enchantmentLevel={}, height={}, width={}, depth={}, directionSign={}",
                enchantmentLevel, heightAxis, widthAxis, depthAxis, directionSign);

        double damage = Math.max(1, 1 * DrillConfig.get().durabilityFactor) * Math.pow(2 * enchantmentLevel + 1, 2) * (enchantmentLevel + 1);

        if ((heldItemStack.getMaxDamage() - heldItemStack.getDamage()) <= damage) {
            player.sendMessage(Text.literal("§cNot enough durability to Drill."), true);
            return true;
        }

        for (int h = -enchantmentLevel; h <= enchantmentLevel; h++) {
            for (int w = -enchantmentLevel; w <= enchantmentLevel; w++) {
                for (int d = 0; d <= enchantmentLevel; d++) {
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
        if (BreakingPositions.isEmpty()) return true;
        LOGGER.debug("Breaking positions: {}", BreakingPositions);
        TASKS.add(new DrillTask(world, player, BreakingPositions, heldItemStack.copy()));

        return true;
    }

    private int getDrillLevel(World world, ItemStack stack) {
        return world.getRegistryManager()
                .getOrThrow(RegistryKeys.ENCHANTMENT)
                .getOptional(ModEnchantments.DRILL)
                .map(entry -> EnchantmentHelper.getLevel(entry, stack))
                .orElse(0);
    }

    public static void onTick(MinecraftServer server) {
        if (TASKS.isEmpty()) return;

        Iterator<DrillTask> iterator = TASKS.iterator();
        while (iterator.hasNext()) {
            DrillTask task = iterator.next();
            if (task.isComplete()) {
                iterator.remove();
            } else {
                task.process();
            }
        }
    }

    private static class DrillTask {
        private final World world;
        private final PlayerEntity player;
        private final ItemStack tool;
        private final Iterator<BlockPos> blockIterator;

        public DrillTask(World world, PlayerEntity player, Set<BlockPos> blocks, ItemStack tool) {
            this.world = world;
            this.player = player;
            this.tool = tool;
            this.blockIterator = blocks.iterator();
        }

        public boolean isComplete() {
            return !blockIterator.hasNext();
        }

        public void process() {
            int processedCount = 0;
            int blocksPerTick = DrillConfig.get().blocksPerTick;

            // Process up to BLOCKS_PER_TICK or until iterator is empty
            while (blockIterator.hasNext() && processedCount < blocksPerTick) {
                BlockPos pos = blockIterator.next();
                processedCount++;

                BlockState state = world.getBlockState(pos);

                // Re-check validation inside the tick loop as world state might have changed
                if (state.isAir() || state.getHardness(world, pos) == -1.0f || DENY_LIST.contains(state.getBlock())) {
                    continue;
                }

                if (!state.isIn(BlockTags.PICKAXE_MINEABLE) || !tool.isSuitableFor(state)) {
                    continue;
                }

                if (tool.canMine(state, world, pos, player)) {
                    BlockEntity blockEntity = world.getBlockEntity(pos);
                    Block.dropStacks(state, world, pos, blockEntity, player, tool);
                    if (world.breakBlock(pos, false)) {
                        LOGGER.debug("Block at {} broken by drill task", pos);
                        LedgerLogger.insert(world, player, pos, state, null, blockEntity);
                    }
                    if (!player.isCreative()) {
                        /// Ensure the player is still holding the same tool type
                        ItemStack currentStack = player.getMainHandStack();
                        if (currentStack.getItem() == tool.getItem()) {
                            int damage = (int) Math.max(1, 1 * DrillConfig.get().durabilityFactor);
                            currentStack.damage(damage, player, EquipmentSlot.MAINHAND);
                        }
                    } else {
                        LOGGER.debug("Drill Enchant: {} is in creative, ignoring damage", player.getName());
                    }
                }
            }
        }
    }
}
