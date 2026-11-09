package com.winterapocalypse.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.winterapocalypse.temperature.TemperatureManager;
import com.winterapocalypse.world.ColdManager;
import com.winterapocalypse.world.ColdWorldData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

/**
 * /winter status                — день зимы, радиус, температура (всем)
 * /winter locate                — координаты блоков холода (оп)
 * /winter setday <дни>          — перемотать возраст зимы, для тестов (оп)
 */
public class WinterCommand {
    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("winter")
                .then(Commands.literal("status").executes(WinterCommand::status))
                .then(Commands.literal("locate").requires(s -> s.hasPermission(2)).executes(WinterCommand::locate))
                .then(Commands.literal("setday").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("day", DoubleArgumentType.doubleArg(0.0))
                                .executes(WinterCommand::setDay))));
    }

    private static String f(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private static int status(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        ServerLevel ow = ctx.getSource().getServer().overworld();
        ColdWorldData data = ColdWorldData.get(ow);
        long t = ow.getDayTime();

        double nearest = -1;
        double best = Double.MAX_VALUE;
        for (ColdWorldData.Source s : data.sources) {
            double dx = p.getX() - s.pos.getX(), dz = p.getZ() - s.pos.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < best) {
                best = dist;
                nearest = ColdManager.radius(ColdManager.age(s.startTime, t));
            }
        }
        double amb = TemperatureManager.getAmbient(p.serverLevel(), p.blockPosition());
        final double nr = nearest;
        ctx.getSource().sendSuccess(() -> Component.translatable("command.winterapocalypse.status",
                f(ColdManager.globalAge(data, t)), data.sources.size(), nr < 0 ? "-" : f(nr),
                f(amb), f(TemperatureManager.getBody(p))), false);
        return 1;
    }

    private static int locate(CommandContext<CommandSourceStack> ctx) {
        ServerLevel ow = ctx.getSource().getServer().overworld();
        ColdWorldData data = ColdWorldData.get(ow);
        if (data.sources.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("command.winterapocalypse.none"), false);
            return 0;
        }
        long t = ow.getDayTime();
        for (ColdWorldData.Source s : data.sources) {
            double r = ColdManager.radius(ColdManager.age(s.startTime, t));
            ctx.getSource().sendSuccess(() -> Component.translatable("command.winterapocalypse.locate",
                    s.pos.getX(), s.pos.getY(), s.pos.getZ(), f(r)), false);
        }
        return data.sources.size();
    }

    private static int setDay(CommandContext<CommandSourceStack> ctx) {
        double day = DoubleArgumentType.getDouble(ctx, "day");
        ServerLevel ow = ctx.getSource().getServer().overworld();
        ColdWorldData data = ColdWorldData.get(ow);
        long t = ow.getDayTime();
        long start = t - (long) (day * 24000.0);
        data.startDayTime = start;
        for (ColdWorldData.Source s : data.sources) s.startTime = start;
        data.setDirty();
        ctx.getSource().sendSuccess(() -> Component.translatable("command.winterapocalypse.setday", f(day)), true);
        return 1;
    }
}
