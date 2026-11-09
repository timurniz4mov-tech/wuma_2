package com.winterapocalypse.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Одежда с теплоизоляцией (°C) и защитой от метели (0..1). */
public class WinterArmorItem extends ArmorItem {
    public final double warmth;
    public final double windProtection;

    public WinterArmorItem(ArmorMaterial material, Type type, Properties props, double warmth, double windProtection) {
        super(material, type, props);
        this.warmth = warmth;
        this.windProtection = windProtection;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.winterapocalypse.warmth", (int) warmth)
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.winterapocalypse.wind", (int) Math.round(windProtection * 100))
                .withStyle(ChatFormatting.BLUE));
    }
}
