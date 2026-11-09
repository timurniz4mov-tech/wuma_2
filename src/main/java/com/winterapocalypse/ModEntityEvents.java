package com.winterapocalypse;

import com.winterapocalypse.entity.FrostGiant;
import com.winterapocalypse.entity.FrostMonster;
import com.winterapocalypse.entity.FrostWalker;
import com.winterapocalypse.entity.WinterMaster;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WinterApocalypse.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntityEvents {

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent e) {
        e.put(ModEntities.FROST_WALKER.get(), FrostWalker.createAttributes().build());
        e.put(ModEntities.FROST_GIANT.get(), FrostGiant.createAttributes().build());
        e.put(ModEntities.WINTER_MASTER.get(), WinterMaster.createAttributes().build());
    }

    @SubscribeEvent
    public static void spawnPlacements(SpawnPlacementRegisterEvent e) {
        e.register(ModEntities.FROST_WALKER.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, FrostMonster::checkSnowSpawn,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
        e.register(ModEntities.FROST_GIANT.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, FrostGiant::checkGiantSpawn,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
}
