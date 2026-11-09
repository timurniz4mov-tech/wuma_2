package com.winterapocalypse.network;

import com.winterapocalypse.client.ClientTempData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Сервер -> клиент: температура тела, воздуха, день зимы, метель и защита от неё. */
public class TempPacket {
    public final float body;
    public final float ambient;
    public final float day;
    public final float blizzard;
    public final float protection;

    public TempPacket(float body, float ambient, float day, float blizzard, float protection) {
        this.body = body;
        this.ambient = ambient;
        this.day = day;
        this.blizzard = blizzard;
        this.protection = protection;
    }

    public static void encode(TempPacket p, FriendlyByteBuf buf) {
        buf.writeFloat(p.body);
        buf.writeFloat(p.ambient);
        buf.writeFloat(p.day);
        buf.writeFloat(p.blizzard);
        buf.writeFloat(p.protection);
    }

    public static TempPacket decode(FriendlyByteBuf buf) {
        return new TempPacket(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    public static void handle(TempPacket p, Supplier<NetworkEvent.Context> ctx) {
        // ClientTempData не содержит клиентских импортов, безопасно вызывать отсюда
        ctx.get().enqueueWork(() -> ClientTempData.set(p.body, p.ambient, p.day, p.blizzard, p.protection));
        ctx.get().setPacketHandled(true);
    }
}
