package net.eligibbs.uti;

import dev.ftb.mods.ftbultimine.api.blockbreaking.BlockBreakHandler;
import dev.ftb.mods.ftbultimine.api.blockbreaking.RegisterBlockBreakHandlerEvent;
import dev.ftb.mods.ftbultimine.api.shape.Shape;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.Collections;
import java.util.List;

@Mod(UtiMod.MODID)
public class UtiMod {
    public static final String MODID = "uti";

    public UtiMod() {
        RegisterBlockBreakHandlerEvent.REGISTER.register(registry -> {
            registry.registerHandler(InventoryBlockBreakHandler.INSTANCE);
        });
    }

    public enum InventoryBlockBreakHandler implements BlockBreakHandler {
        INSTANCE;

        @Override
        public Result breakBlock(Player player, BlockPos pos, BlockState state, Shape shape, BlockHitResult hitResult) {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return Result.PASS;
            }

            ServerLevel level = serverPlayer.serverLevel();

            // Unbreakable blocks should not be broken
            if (state.getDestroySpeed(level, pos) < 0) {
                return Result.FAIL;
            }

            if (serverPlayer.isCreative()) {
                boolean destroyed = level.destroyBlock(pos, false, serverPlayer);
                return destroyed ? Result.SUCCESS : Result.FAIL;
            }

            ItemStack tool = serverPlayer.getMainHandItem();
            boolean canHarvest = !state.requiresCorrectToolForDrops() || serverPlayer.hasCorrectToolForDrops(state);
            BlockEntity blockEntity = level.getBlockEntity(pos);

            List<ItemStack> drops = canHarvest
                    ? Block.getDrops(state, level, pos, blockEntity, serverPlayer, tool)
                    : Collections.emptyList();

            int exp = canHarvest
                    ? state.getExpDrop(level, pos, blockEntity, serverPlayer, tool)
                    : 0;

            boolean destroyed = level.destroyBlock(pos, false, serverPlayer);
            if (!destroyed) {
                return Result.FAIL;
            }

            serverPlayer.awardStat(Stats.BLOCK_MINED.get(state.getBlock()));

            for (ItemStack drop : drops) {
                if (!drop.isEmpty()) {
                    ItemHandlerHelper.giveItemToPlayer(serverPlayer, drop.copy());
                }
            }

            if (exp > 0) {
                serverPlayer.giveExperiencePoints(exp);
            }

            return Result.SUCCESS;
        }
    }
}
