package com.winterapocalypse.block;

import com.winterapocalypse.ModItems;
import com.winterapocalypse.world.ColdManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Locale;

public class ColdBlock extends Block {
    public ColdBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (!level.isClientSide && level instanceof ServerLevel sl && !oldState.is(state.getBlock())) {
            ColdManager.registerSource(sl, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!level.isClientSide && level instanceof ServerLevel sl && !newState.is(state.getBlock())) {
            ColdManager.unregisterSource(sl, pos);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    /** ПКМ пустой рукой — показать радиус и остаток замедления. С Ядром тепла — отдаём предмету. */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).is(ModItems.WARMTH_CORE.get())) return InteractionResult.PASS;
        if (!level.isClientSide && level instanceof ServerLevel sl) {
            double[] info = ColdManager.info(sl, pos);
            if (info != null) {
                player.displayClientMessage(Component.translatable("msg.winterapocalypse.cold_info",
                        String.format(Locale.ROOT, "%.0f", info[0]),
                        String.format(Locale.ROOT, "%.1f", info[1])), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 3; i++) {
            level.addParticle(ParticleTypes.SNOWFLAKE,
                    pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 3.0,
                    pos.getY() + 0.2 + random.nextDouble() * 2.5,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 3.0,
                    0.0, -0.02, 0.0);
        }
    }
}
