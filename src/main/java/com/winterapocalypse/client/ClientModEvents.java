package com.winterapocalypse.client;

import com.winterapocalypse.ModEntities;
import com.winterapocalypse.WinterApocalypse;
import com.winterapocalypse.entity.FrostMonster;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WinterApocalypse.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("temperature", TemperatureHud.OVERLAY);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.<FrostMonster>registerEntityRenderer(ModEntities.FROST_WALKER.get(),
                ctx -> new FrostMonsterRenderer(ctx, "frost_walker", 0.9F, 0.5F));
        event.<FrostMonster>registerEntityRenderer(ModEntities.FROST_GIANT.get(),
                ctx -> new FrostMonsterRenderer(ctx, "frost_giant", 2.1F, 1.4F));
        event.registerEntityRenderer(ModEntities.WINTER_MASTER.get(), WinterMasterRenderer::new);
    }
}
