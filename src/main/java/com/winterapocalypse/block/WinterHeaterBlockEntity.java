package com.winterapocalypse.block;

import com.winterapocalypse.ModBlockEntities;
import com.winterapocalypse.world.HeaterRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class WinterHeaterBlockEntity extends BlockEntity {
    public static final int MAX_BURN = 24000; // 20 минут топлива

    private int burnTime = 0;

    public WinterHeaterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEATER.get(), pos, state);
    }

    public boolean isBurning() {
        return burnTime > 0;
    }

    public int getBurnTime() {
        return burnTime;
    }

    public void addFuel(int ticks) {
        burnTime = Math.min(MAX_BURN, burnTime + ticks);
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState s = getBlockState();
            if (!s.getValue(WinterHeaterBlock.LIT)) {
                level.setBlock(worldPosition, s.setValue(WinterHeaterBlock.LIT, true), 3);
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) HeaterRegistry.add(level, worldPosition);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) HeaterRegistry.remove(level, worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Burn", burnTime);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        burnTime = tag.getInt("Burn");
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WinterHeaterBlockEntity be) {
        if (be.burnTime > 0) {
            be.burnTime--;
            if (!state.getValue(WinterHeaterBlock.LIT)) {
                level.setBlock(pos, state.setValue(WinterHeaterBlock.LIT, true), 3);
            }
            // таем снег вокруг
            if (level.getGameTime() % 20 == 0) {
                RandomSource r = level.random;
                for (int i = 0; i < 16; i++) {
                    BlockPos p = pos.offset(r.nextInt(13) - 6, r.nextInt(6) - 2, r.nextInt(13) - 6);
                    BlockState s = level.getBlockState(p);
                    if (s.is(Blocks.SNOW) || s.is(Blocks.SNOW_BLOCK)) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
            if (be.burnTime % 200 == 0) be.setChanged();
        } else if (state.getValue(WinterHeaterBlock.LIT)) {
            level.setBlock(pos, state.setValue(WinterHeaterBlock.LIT, false), 3);
        }
    }
}
