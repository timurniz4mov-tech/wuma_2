package com.winterapocalypse.item;

import com.winterapocalypse.ModBlocks;
import com.winterapocalypse.WinterConfig;
import com.winterapocalypse.world.ColdManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** ПКМ по Блоку холода: замедляет рост его зоны на несколько дней. */
public class WarmthCoreItem extends Item {
    public WarmthCoreItem(Properties props) {
        super(props);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        if (!level.getBlockState(pos).is(ModBlocks.COLD_BLOCK.get())) return InteractionResult.PASS;

        if (!level.isClientSide && level instanceof ServerLevel sl) {
            if (!ColdManager.slowSource(sl, pos)) return InteractionResult.FAIL;

            Player p = ctx.getPlayer();
            if (p != null) {
                if (!p.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
                p.displayClientMessage(Component.translatable("msg.winterapocalypse.core_used",
                        String.format("%.0f", WinterConfig.CORE_SLOW_DAYS.get()),
                        String.format("%.0f", WinterConfig.CORE_SLOW_FACTOR.get() * 100)), true);
            }
            sl.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    40, 0.6, 0.6, 0.6, 0.05);
            sl.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.2F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.winterapocalypse.warmth_core").withStyle(ChatFormatting.GOLD));
    }
}
