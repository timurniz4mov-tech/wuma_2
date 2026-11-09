package com.winterapocalypse;

import com.winterapocalypse.command.WinterCommand;
import com.winterapocalypse.temperature.TemperatureManager;
import com.winterapocalypse.world.ColdManager;
import com.winterapocalypse.world.HeaterRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WinterApocalypse.MODID)
public class ServerEvents {

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.level instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
            ColdManager.tick(level);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) return;
        if (event.player instanceof ServerPlayer sp) {
            TemperatureManager.tickPlayer(sp);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            TemperatureManager.reset(sp);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            CompoundTag d = TemperatureManager.persisted(sp);
            if (!d.getBoolean("wa_intro")) {
                d.putBoolean("wa_intro", true);
                sp.sendSystemMessage(Component.translatable("msg.winterapocalypse.intro"));
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        HeaterRegistry.clear(); // статический реестр не должен «протекать» между мирами
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        WinterCommand.register(event.getDispatcher());
    }
}
