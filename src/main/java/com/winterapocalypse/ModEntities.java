package com.winterapocalypse;

import com.winterapocalypse.entity.FrostGiant;
import com.winterapocalypse.entity.FrostWalker;
import com.winterapocalypse.entity.WinterMaster;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, WinterApocalypse.MODID);

    /** Обмороженный — ростом с игрока (1.8). */
    public static final RegistryObject<EntityType<FrostWalker>> FROST_WALKER = ENTITIES.register("frost_walker",
            () -> EntityType.Builder.of(FrostWalker::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F).clientTrackingRange(8).build("frost_walker"));

    /** Ледяной великан — 4.2 блока. */
    public static final RegistryObject<EntityType<FrostGiant>> FROST_GIANT = ENTITIES.register("frost_giant",
            () -> EntityType.Builder.of(FrostGiant::new, MobCategory.MONSTER)
                    .sized(1.9F, 4.2F).clientTrackingRange(10).build("frost_giant"));

    /** Мастер по зиме — торговец. */
    public static final RegistryObject<EntityType<WinterMaster>> WINTER_MASTER = ENTITIES.register("winter_master",
            () -> EntityType.Builder.of(WinterMaster::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).clientTrackingRange(10).build("winter_master"));
}
