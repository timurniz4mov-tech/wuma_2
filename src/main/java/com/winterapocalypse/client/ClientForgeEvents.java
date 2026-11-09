package com.winterapocalypse.client;

import com.winterapocalypse.WinterApocalypse;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Снегопад из частиц при <0 °C и метель (густой туман + ветер). */
@Mod.EventBusSubscriber(modid = WinterApocalypse.MODID, value = Dist.CLIENT)
public class ClientForgeEvents {

    /** Метель с учётом защиты: парка прячет от ветра и ослабляет туман. */
    private static float fogStrength() {
        return ClientTempData.blizzard * (1.0f - 0.7f * ClientTempData.protection);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;

        float amb = ClientTempData.ambient;
        if (amb > 0f) return;
        if (!mc.level.canSeeSky(mc.player.blockPosition())) return;

        float bl = ClientTempData.blizzard;
        int n = (amb < -20f ? 5 : amb < -10f ? 3 : 1) + (int) (bl * 10);
        double wind = 0.04 + bl * 0.35;
        RandomSource r = mc.level.random;
        for (int i = 0; i < n; i++) {
            double x = mc.player.getX() + (r.nextDouble() - 0.5) * 28.0;
            double y = mc.player.getY() + r.nextDouble() * 8.0;
            double z = mc.player.getZ() + (r.nextDouble() - 0.5) * 28.0;
            mc.level.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, wind, -0.07, wind * 0.4);
        }
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        float k = fogStrength();
        if (k < 0.02f) return;
        if (event.getType() != FogType.NONE) return; // не под водой / лавой

        float oldFar = event.getFarPlaneDistance();
        float far = Mth.lerp(k, oldFar, 22.0f);
        if (far >= oldFar) return;
        event.setNearPlaneDistance(Mth.lerp(k, event.getNearPlaneDistance(), 0.0f));
        event.setFarPlaneDistance(far);
        event.setCanceled(true); // применить наши значения
    }
}
