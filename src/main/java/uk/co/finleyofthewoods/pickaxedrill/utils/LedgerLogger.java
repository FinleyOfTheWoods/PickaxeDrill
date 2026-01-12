package uk.co.finleyofthewoods.pickaxedrill.utils;

import com.github.quiltservertools.ledger.Ledger;
import com.github.quiltservertools.ledger.actions.ActionType;
import com.github.quiltservertools.ledger.actionutils.ActionFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import uk.co.finleyofthewoods.pickaxedrill.config.DrillConfig;

public class LedgerLogger {
    public static void insert(World world,
                              PlayerEntity player,
                              BlockPos pos,
                              BlockState state,
                              ItemPlacementContext context,
                              BlockEntity entity) {
        if (!DrillConfig.get().enableLedger) return;
        // Create action
        ActionType action = ActionFactory.INSTANCE.blockBreakAction(world, pos, state, player.getName().toString(), entity);
        // Log the action
        Ledger.getApi().logAction(action);
    }
}
