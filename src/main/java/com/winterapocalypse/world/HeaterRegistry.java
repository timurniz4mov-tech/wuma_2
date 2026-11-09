package com.winterapocalypse.world;

import com.winterapocalypse.block.WinterHeaterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Реестр загруженных печей-обогревателей (серверная сторона) — чтобы быстро считать тепло. */
public final class HeaterRegistry {
    public static final double RADIUS = 10.0;
    public static final double MAX_HEAT = 38.0; // °C прибавки к ощущаемой температуре вплотную к печи

    private static final Map<ResourceKey<Level>, Set<BlockPos>> HEATERS = new ConcurrentHashMap<>();

    private HeaterRegistry() {}

    public static void add(Level level, BlockPos pos) {
        HEATERS.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet()).add(pos.immutable());
    }

    public static void remove(Level level, BlockPos pos) {
        Set<BlockPos> s = HEATERS.get(level.dimension());
        if (s != null) s.remove(pos);
    }

    public static void clear() {
        HEATERS.clear();
    }

    private static WinterHeaterBlockEntity burning(Level level, BlockPos hp) {
        if (!level.isLoaded(hp)) return null;
        BlockEntity be = level.getBlockEntity(hp);
        if (be instanceof WinterHeaterBlockEntity h && h.isBurning()) return h;
        return null;
    }

    /** Тепло (в °C) от всех горящих печей в точке. */
    public static double heatAt(Level level, BlockPos pos) {
        Set<BlockPos> set = HEATERS.get(level.dimension());
        if (set == null || set.isEmpty()) return 0.0;
        double sum = 0.0;
        for (BlockPos hp : set) {
            double d = Math.sqrt(hp.distSqr(pos));
            if (d > RADIUS) continue;
            if (burning(level, hp) == null) continue;
            double k = d / RADIUS;
            sum += MAX_HEAT * (1.0 - k * k);
        }
        return Math.min(sum, 45.0);
    }

    public static boolean burningNear(Level level, BlockPos pos, double radius) {
        Set<BlockPos> set = HEATERS.get(level.dimension());
        if (set == null || set.isEmpty()) return false;
        for (BlockPos hp : set) {
            if (hp.distSqr(pos) <= radius * radius && burning(level, hp) != null) return true;
        }
        return false;
    }
}
