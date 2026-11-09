package com.winterapocalypse;

import com.winterapocalypse.block.WinterHeaterBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, WinterApocalypse.MODID);

    public static final RegistryObject<BlockEntityType<WinterHeaterBlockEntity>> HEATER =
            BLOCK_ENTITIES.register("winter_heater",
                    () -> BlockEntityType.Builder.of(WinterHeaterBlockEntity::new, ModBlocks.WINTER_HEATER.get())
                            .build(null));
}
