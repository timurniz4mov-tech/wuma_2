package com.winterapocalypse;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WinterApocalypse.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.winterapocalypse"))
                    .icon(() -> new ItemStack(ModItems.COLD_BLOCK_ITEM.get()))
                    .displayItems((params, out) ->
                            ModItems.ITEMS.getEntries().forEach(i -> out.accept(i.get())))
                    .build());
}
