package com.winterapocalypse.block;

import com.winterapocalypse.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ForgeHooks;
import org.jetbrains.annotations.Nullable;

/** Зимняя печь-обогреватель: ПКМ топливом, греет в радиусе 10 блоков, не даёт снегу ложиться рядом. */
public class WinterHeaterBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public WinterHeaterBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    // BaseEntityBlock по умолчанию невидим — без этого блок не отрисуется
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WinterHeaterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, ModBlockEntities.HEATER.get(), WinterHeaterBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof WinterHeaterBlockEntity heater)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);
        int burn = ForgeHooks.getBurnTime(held, RecipeType.SMELTING);

        if (burn > 0) {
            if (heater.getBurnTime() >= WinterHeaterBlockEntity.MAX_BURN - 100) {
                if (!level.isClientSide) {
                    player.displayClientMessage(Component.translatable("msg.winterapocalypse.heater_full"), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!level.isClientSide) {
                heater.addFuel(burn);
                if (!player.getAbilities().instabuild) {
                    ItemStack remainder = held.getCraftingRemainingItem();
                    held.shrink(1);
                    if (!remainder.isEmpty()) {
                        if (held.isEmpty()) player.setItemInHand(hand, remainder);
                        else if (!player.getInventory().add(remainder)) player.drop(remainder, false);
                    }
                }
                level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8F, 1.0F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable("msg.winterapocalypse.heater_burn",
                    heater.getBurnTime() / 20), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        double x = pos.getX() + 0.5, y = pos.getY(), z = pos.getZ() + 0.5;
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMOKE, x, y + 1.05, z, 0.0, 0.06, 0.0);
        }
        if (random.nextInt(6) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.8F, 1.0F, false);
        }
        Direction d = state.getValue(FACING);
        double fx = x + d.getStepX() * 0.52;
        double fz = z + d.getStepZ() * 0.52;
        level.addParticle(ParticleTypes.FLAME, fx + (random.nextDouble() - 0.5) * 0.4, y + 0.3 + random.nextDouble() * 0.3,
                fz + (random.nextDouble() - 0.5) * 0.4, 0.0, 0.0, 0.0);
    }
}
