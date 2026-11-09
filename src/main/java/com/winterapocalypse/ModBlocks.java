package com.winterapocalypse;

import com.winterapocalypse.block.ColdBlock;
import com.winterapocalypse.block.WinterHeaterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, WinterApocalypse.MODID);

    // hardness -1 = нельзя сломать в выживании (в креативе можно)
    public static final RegistryObject<Block> COLD_BLOCK = BLOCKS.register("cold_block",
            () -> new ColdBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> 6)));

    public static final RegistryObject<Block> WINTER_HEATER = BLOCKS.register("winter_heater",
            () -> new WinterHeaterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(WinterHeaterBlock.LIT) ? 14 : 0)));
}
