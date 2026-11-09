package com.winterapocalypse.world;

import com.winterapocalypse.ModBlocks;
import com.winterapocalypse.ModEntities;
import com.winterapocalypse.WinterApocalypse;
import com.winterapocalypse.WinterConfig;
import com.winterapocalypse.entity.WinterMaster;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Вся логика Блока Холода: появление, рост радиуса, замедление, снегопад, метели. */
public final class ColdManager {
    private static final int SCAN = 48; // в каком квадрате вокруг игрока кладём снег

    private ColdManager() {}

    // ------------------------------------------------------------------ формулы

    public static double age(long startTime, long dayTime) {
        return Math.max(0.0, (dayTime - startTime) / 24000.0);
    }

    /** Радиус зоны: растёт всё быстрее с каждым днём (квадратичная формула). */
    public static double radius(double ageDays) {
        double r = WinterConfig.START_RADIUS.get()
                + WinterConfig.GROWTH_PER_DAY.get() * ageDays
                + 0.5 * WinterConfig.GROWTH_ACCEL.get() * ageDays * ageDays;
        return Math.min(r, WinterConfig.MAX_RADIUS.get());
    }

    /** Попыток положить снег за цикл — растёт с каждым днём. */
    public static int snowAttempts(double ageDays) {
        double a = WinterConfig.SNOW_ATTEMPTS_BASE.get() * (1.0 + ageDays * WinterConfig.SNOW_SPEED_GROWTH.get());
        return (int) Math.min(a, WinterConfig.SNOW_ATTEMPTS_MAX.get());
    }

    /** Максимальная толщина снега (в слоях) — тоже растёт с днями. */
    public static int maxLayers(double ageDays) {
        return Math.max(1, Math.min(WinterConfig.MAX_SNOW_LAYERS.get(), 1 + (int) (ageDays * 2)));
    }

    /** Насколько °C холод от источника с таким радиусом — чем больше зона, тем холоднее. */
    public static double coldDrop(double radius) {
        return Math.min(WinterConfig.COLD_DROP_MAX.get(),
                WinterConfig.COLD_BASE_DROP.get() + radius * WinterConfig.COLD_DROP_PER_BLOCK.get());
    }

    /** Падение температуры от одного источника в точке (x, z). */
    public static double sourceDrop(ColdWorldData.Source s, long dayTime, double x, double z) {
        double r = radius(age(s.startTime, dayTime));
        double dx = x - (s.pos.getX() + 0.5);
        double dz = z - (s.pos.getZ() + 0.5);
        double d = Math.sqrt(dx * dx + dz * dz);
        double fade = 24.0; // плавный край за границей зоны
        if (d > r + fade) return 0.0;
        double f = d <= r ? 1.0 - 0.4 * (d / r) : 0.6 * (1.0 - (d - r) / fade);
        return coldDrop(r) * f;
    }

    public static double globalAge(ColdWorldData data, long dayTime) {
        return age(data.startDayTime, dayTime);
    }

    /** Находится ли точка в зоне (с запасом margin) любого блока холода. */
    public static boolean isInZone(ServerLevel level, BlockPos pos, double margin) {
        if (level.dimension() != Level.OVERWORLD) return false;
        ColdWorldData data = ColdWorldData.get(level);
        long t = level.getDayTime();
        for (ColdWorldData.Source s : data.sources) {
            double r = radius(age(s.startTime, t)) + margin;
            double dx = pos.getX() - s.pos.getX();
            double dz = pos.getZ() - s.pos.getZ();
            if (dx * dx + dz * dz <= r * r) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ метель

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /**
     * Интенсивность метели 0..1. Детерминирована (от сида мира и номера дня), ничего не хранит.
     * Метели случаются чаще с каждым днём зимы; длятся 3000–6500 тиков, с плавным началом и концом.
     */
    public static double blizzardIntensity(ServerLevel overworld, ColdWorldData data) {
        if (!WinterConfig.BLIZZARD_ENABLED.get()) return 0.0;
        long dayTime = overworld.getDayTime();
        long day = Math.floorDiv(dayTime, 24000L);
        long tod = Math.floorMod(dayTime, 24000L);

        long h = mix(day * 0x9E3779B97F4A7C15L + overworld.getSeed());
        double roll = (h >>> 40) / (double) (1L << 24);
        double chance = Math.min(0.85, WinterConfig.BLIZZARD_BASE_CHANCE.get() + 0.04 * globalAge(data, dayTime));
        if (roll > chance) return 0.0;

        long h2 = mix(h);
        long start = Math.floorMod(h2, 15000L);
        long len = 3000L + Math.floorMod(h2 >>> 20, 3500L);
        if (tod < start || tod >= start + len) return 0.0;

        double ramp = 300.0;
        return Math.max(0.0, Math.min(1.0, Math.min((tod - start) / ramp, (start + len - tod) / ramp)));
    }

    // ------------------------------------------------------------------ регистрация / замедление

    public static void registerSource(ServerLevel level, BlockPos pos) {
        if (level.dimension() != Level.OVERWORLD) return;
        ColdWorldData data = ColdWorldData.get(level);
        for (ColdWorldData.Source s : data.sources) {
            if (s.pos.equals(pos)) return;
        }
        data.sources.add(new ColdWorldData.Source(pos.immutable(), level.getDayTime()));
        data.setDirty();
    }

    public static void unregisterSource(ServerLevel level, BlockPos pos) {
        if (level.dimension() != Level.OVERWORLD) return;
        ColdWorldData data = ColdWorldData.get(level);
        if (data.sources.removeIf(s -> s.pos.equals(pos))) {
            data.setDirty();
        }
    }

    /** Ядро тепла: замедляет рост зоны. Возвращает false, если это не источник. */
    public static boolean slowSource(ServerLevel level, BlockPos pos) {
        if (level.dimension() != Level.OVERWORLD) return false;
        ColdWorldData data = ColdWorldData.get(level);
        for (ColdWorldData.Source s : data.sources) {
            if (!s.pos.equals(pos)) continue;
            long now = level.getDayTime();
            long add = (long) (WinterConfig.CORE_SLOW_DAYS.get() * 24000.0);
            long cap = now + (long) (WinterConfig.CORE_MAX_STACK_DAYS.get() * 24000.0);
            s.slowEnd = Math.min(Math.max(now, s.slowEnd) + add, cap);
            s.slowFactor = WinterConfig.CORE_SLOW_FACTOR.get().floatValue();
            data.setDirty();
            return true;
        }
        return false;
    }

    /** {радиус, дней замедления осталось} или null. */
    public static double[] info(ServerLevel level, BlockPos pos) {
        if (level.dimension() != Level.OVERWORLD) return null;
        ColdWorldData data = ColdWorldData.get(level);
        long now = level.getDayTime();
        for (ColdWorldData.Source s : data.sources) {
            if (s.pos.equals(pos)) {
                return new double[]{radius(age(s.startTime, now)), Math.max(0L, s.slowEnd - now) / 24000.0};
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ тик

    public static void tick(ServerLevel level) {
        ColdWorldData data = ColdWorldData.get(level);
        long gameTime = level.getGameTime();

        if (!data.initialized) {
            if (gameTime < 20) return;
            data.initialized = true;
            data.startDayTime = level.getDayTime();
            data.setDirty();
            for (int i = 0; i < WinterConfig.SOURCE_COUNT.get(); i++) {
                spawnSource(level);
            }
            return;
        }

        if (!data.masterSpawned && gameTime > 40) {
            data.masterSpawned = true;
            data.setDirty();
            if (WinterConfig.SPAWN_MASTER.get()) spawnMaster(level);
        }

        if (gameTime % 4 != 0) return;

        long dayTime = level.getDayTime();

        // замедление роста Ядром тепла: «отматываем» возраст источника
        if (data.lastTick < 0) data.lastTick = dayTime;
        long last = data.lastTick;
        data.lastTick = dayTime;
        if (dayTime > last) {
            for (ColdWorldData.Source s : data.sources) {
                if (s.slowEnd > last) {
                    long portion = Math.min(dayTime, s.slowEnd) - last;
                    if (portion > 0) {
                        s.startTime += (long) (portion * (1.0 - s.slowFactor));
                        data.setDirty();
                    }
                }
            }
        }

        if (data.sources.isEmpty()) return;

        RandomSource rnd = level.random;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) continue;
            for (ColdWorldData.Source s : data.sources) {
                double age = age(s.startTime, dayTime);
                double r = radius(age);
                double dx = p.getX() - s.pos.getX();
                double dz = p.getZ() - s.pos.getZ();
                if (Math.sqrt(dx * dx + dz * dz) > r + SCAN) continue;

                int attempts = snowAttempts(age);
                int maxLayers = maxLayers(age);
                int px = (int) Math.floor(p.getX());
                int pz = (int) Math.floor(p.getZ());

                for (int i = 0; i < attempts; i++) {
                    int x = px + rnd.nextInt(SCAN * 2 + 1) - SCAN;
                    int z = pz + rnd.nextInt(SCAN * 2 + 1) - SCAN;
                    double ddx = x + 0.5 - (s.pos.getX() + 0.5);
                    double ddz = z + 0.5 - (s.pos.getZ() + 0.5);
                    if (ddx * ddx + ddz * ddz > r * r) continue;
                    if (!level.hasChunkAt(new BlockPos(x, 64, z))) continue;
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                    snowify(level, new BlockPos(x, y, z), maxLayers);
                }
            }
        }
    }

    // ------------------------------------------------------------------ появление

    private static void spawnSource(ServerLevel level) {
        RandomSource rnd = level.random;
        BlockPos spawn = level.getSharedSpawnPos();
        int min = WinterConfig.MIN_SPAWN_DIST.get();
        int max = Math.max(min + 1, WinterConfig.MAX_SPAWN_DIST.get());

        BlockPos last = spawn.offset(min, 0, 0);
        for (int i = 0; i < 30; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2.0;
            double dist = min + rnd.nextDouble() * (max - min);
            int x = spawn.getX() + (int) Math.round(Math.cos(ang) * dist);
            int z = spawn.getZ() + (int) Math.round(Math.sin(ang) * dist);

            level.getChunk(x >> 4, z >> 4); // принудительно загрузить/сгенерировать чанк
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            last = pos;

            BlockState below = level.getBlockState(pos.below());
            if (!below.getFluidState().isEmpty()) continue; // не в воду
            if (!below.isFaceSturdy(level, pos.below(), Direction.UP)) continue;

            place(level, pos);
            return;
        }
        place(level, last);
    }

    private static void place(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, ModBlocks.COLD_BLOCK.get().defaultBlockState(), 3);
        WinterApocalypse.LOGGER.info("[WinterApocalypse] Cold Block placed at {} {} {}",
                pos.getX(), pos.getY(), pos.getZ());
    }

    /** Мастер по зиме появляется в 3–8 блоках от спавна. */
    private static void spawnMaster(ServerLevel level) {
        RandomSource rnd = level.random;
        BlockPos spawn = level.getSharedSpawnPos();
        for (int i = 0; i < 30; i++) {
            int x = spawn.getX() + rnd.nextInt(17) - 8;
            int z = spawn.getZ() + rnd.nextInt(17) - 8;
            if (Math.abs(x - spawn.getX()) < 3 && Math.abs(z - spawn.getZ()) < 3) continue;

            level.getChunk(x >> 4, z >> 4);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) continue;

            WinterMaster master = ModEntities.WINTER_MASTER.get().create(level);
            if (master == null) return;
            master.moveTo(x + 0.5, y, z + 0.5, rnd.nextFloat() * 360.0F, 0.0F);
            master.setPersistenceRequired();
            level.addFreshEntity(master);
            WinterApocalypse.LOGGER.info("[WinterApocalypse] Winter Master spawned at {} {} {}", x, y, z);
            return;
        }
    }

    // ------------------------------------------------------------------ снег

    /** Кладёт/добавляет слой снега в самой верхней точке колонки. {@code p} — первая свободная позиция сверху. */
    private static void snowify(ServerLevel level, BlockPos p, int maxLayers) {
        // рядом с источниками света и печами снег не ложится (как и в ваниле, там он тает)
        if (level.getBrightness(LightLayer.BLOCK, p) > 11) return;
        if (HeaterRegistry.burningNear(level, p, 7.0)) return;

        BlockState at = level.getBlockState(p);
        BlockPos belowPos = p.below();
        BlockState below = level.getBlockState(belowPos);

        // 1. Уже лежит слой снега -> делаем толще
        if (at.is(Blocks.SNOW)) {
            int layers = at.getValue(SnowLayerBlock.LAYERS);
            int total = layers + stackedBelow(level, belowPos);
            if (total >= maxLayers) return;
            if (layers >= 7) {
                level.setBlock(p, Blocks.SNOW_BLOCK.defaultBlockState(), 2);
            } else {
                level.setBlock(p, at.setValue(SnowLayerBlock.LAYERS, layers + 1), 2);
            }
            return;
        }

        // 2. Вода -> лёд
        if (below.is(Blocks.WATER) && below.getFluidState().isSource()) {
            level.setBlock(belowPos, Blocks.ICE.defaultBlockState(), 3);
            return;
        }

        // 3. Не надстраиваем выше лимита
        if (stackedBelow(level, belowPos) >= maxLayers) return;

        // 4. Свободное место (воздух / заменяемое / растение)
        if (!(at.isAir() || at.canBeReplaced())) {
            if (WinterConfig.KILL_PLANTS.get() && at.getBlock() instanceof BushBlock) {
                // растение погибнет — будет заменено снегом
            } else {
                return;
            }
        }

        BlockState snow = Blocks.SNOW.defaultBlockState();
        if (!snow.canSurvive(level, p)) return;
        level.setBlock(p, snow, 2);
    }

    /** Сколько слоёв снега уже лежит стопкой вниз от этой позиции. */
    private static int stackedBelow(ServerLevel level, BlockPos from) {
        int total = 0;
        BlockPos.MutableBlockPos m = from.mutable();
        for (int i = 0; i < 8; i++) {
            BlockState s = level.getBlockState(m);
            if (s.is(Blocks.SNOW_BLOCK)) {
                total += 8;
            } else if (s.is(Blocks.SNOW)) {
                total += s.getValue(SnowLayerBlock.LAYERS);
            } else {
                break;
            }
            m.move(Direction.DOWN);
        }
        return total;
    }
}
