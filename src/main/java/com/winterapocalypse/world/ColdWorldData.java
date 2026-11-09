package com.winterapocalypse.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Сохраняемые данные мира: все блоки холода и время старта зимы. Хранится в Оверворлде. */
public class ColdWorldData extends SavedData {
    private static final String NAME = "winterapocalypse_cold";

    public static class Source {
        public final BlockPos pos;
        public long startTime;      // dayTime, когда источник появился (сдвигается при замедлении)
        public long slowEnd = 0L;   // до какого dayTime действует замедление Ядром тепла
        public float slowFactor = 1.0F; // скорость роста во время замедления (1 = обычная)

        public Source(BlockPos pos, long startTime) {
            this.pos = pos;
            this.startTime = startTime;
        }
    }

    public final List<Source> sources = new ArrayList<>();
    public boolean initialized = false;
    public boolean masterSpawned = false;
    public long startDayTime = 0L;

    /** Не сохраняется: для расчёта замедления между тиками. */
    public long lastTick = -1L;

    public static ColdWorldData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(ColdWorldData::load, ColdWorldData::new, NAME);
    }

    public static ColdWorldData load(CompoundTag tag) {
        ColdWorldData d = new ColdWorldData();
        d.initialized = tag.getBoolean("initialized");
        d.masterSpawned = tag.getBoolean("masterSpawned");
        d.startDayTime = tag.getLong("startDayTime");
        ListTag list = tag.getList("sources", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            Source s = new Source(new BlockPos(t.getInt("x"), t.getInt("y"), t.getInt("z")), t.getLong("start"));
            s.slowEnd = t.getLong("slowEnd");
            s.slowFactor = t.contains("slowFactor") ? t.getFloat("slowFactor") : 1.0F;
            d.sources.add(s);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("initialized", initialized);
        tag.putBoolean("masterSpawned", masterSpawned);
        tag.putLong("startDayTime", startDayTime);
        ListTag list = new ListTag();
        for (Source s : sources) {
            CompoundTag t = new CompoundTag();
            t.putInt("x", s.pos.getX());
            t.putInt("y", s.pos.getY());
            t.putInt("z", s.pos.getZ());
            t.putLong("start", s.startTime);
            t.putLong("slowEnd", s.slowEnd);
            t.putFloat("slowFactor", s.slowFactor);
            list.add(t);
        }
        tag.put("sources", list);
        return tag;
    }
}
