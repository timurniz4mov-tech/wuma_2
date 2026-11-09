package com.winterapocalypse.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.Locale;

public class TemperatureHud {
    public static final IGuiOverlay OVERLAY = (gui, g, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.player.isSpectator()) return;

        float body = ClientTempData.body;
        float air = ClientTempData.ambient;
        int color = bodyColor(body);
        int x = 8, y = 8;

        g.drawString(mc.font, Component.translatable("hud.winterapocalypse.body",
                String.format(Locale.ROOT, "%.1f", body)), x, y, color);
        g.drawString(mc.font, Component.translatable("hud.winterapocalypse.air",
                String.format(Locale.ROOT, "%.0f", air)), x, y + 11, airColor(air));

        // полоска температуры тела: 28..40 °C
        float f = Math.max(0f, Math.min(1f, (body - 28f) / 12f));
        g.fill(x, y + 24, x + 82, y + 30, 0xFF101010);
        g.fill(x + 1, y + 25, x + 1 + (int) (80 * f), y + 29, color);

        g.drawString(mc.font, Component.translatable("hud.winterapocalypse.day",
                String.format(Locale.ROOT, "%.1f", ClientTempData.day)), x, y + 34, 0xFFAAAAAA);

        if (ClientTempData.blizzard > 0.05f) {
            boolean safe = ClientTempData.protection >= 0.95f;
            g.drawString(mc.font, Component.translatable("hud.winterapocalypse.blizzard",
                    Math.round(ClientTempData.protection * 100f)), x, y + 45, safe ? 0xFF88FFAA : 0xFFFF8888);
        }
    };

    private static int bodyColor(float b) {
        if (b > 38.0f) return 0xFFFF5555;   // перегрев
        if (b >= 36.0f) return 0xFF55FF55;  // норма
        if (b >= 35.0f) return 0xFFFFFF55;  // прохладно
        if (b >= 33.0f) return 0xFF55FFFF;  // замерзаешь
        return 0xFF5555FF;                  // опасное переохлаждение
    }

    private static int airColor(float a) {
        if (a < -10f) return 0xFF8888FF;
        if (a < 5f) return 0xFF88DDFF;
        if (a > 30f) return 0xFFFFAA55;
        return 0xFFFFFFFF;
    }
}
