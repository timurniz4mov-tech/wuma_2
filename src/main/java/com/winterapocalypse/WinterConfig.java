package com.winterapocalypse;

import net.minecraftforge.common.ForgeConfigSpec;

/** Все числа можно менять в config/winterapocalypse-common.toml */
public class WinterConfig {
    public static final ForgeConfigSpec SPEC;

    // Блок холода
    public static final ForgeConfigSpec.IntValue SOURCE_COUNT;
    public static final ForgeConfigSpec.IntValue MIN_SPAWN_DIST;
    public static final ForgeConfigSpec.IntValue MAX_SPAWN_DIST;

    // Рост зоны
    public static final ForgeConfigSpec.DoubleValue START_RADIUS;
    public static final ForgeConfigSpec.DoubleValue GROWTH_PER_DAY;
    public static final ForgeConfigSpec.DoubleValue GROWTH_ACCEL;
    public static final ForgeConfigSpec.DoubleValue MAX_RADIUS;

    // Снег
    public static final ForgeConfigSpec.IntValue SNOW_ATTEMPTS_BASE;
    public static final ForgeConfigSpec.DoubleValue SNOW_SPEED_GROWTH;
    public static final ForgeConfigSpec.IntValue SNOW_ATTEMPTS_MAX;
    public static final ForgeConfigSpec.IntValue MAX_SNOW_LAYERS;
    public static final ForgeConfigSpec.BooleanValue KILL_PLANTS;

    // Температура мира
    public static final ForgeConfigSpec.DoubleValue GLOBAL_COOLING_PER_DAY;
    public static final ForgeConfigSpec.DoubleValue GLOBAL_COOLING_MAX;
    public static final ForgeConfigSpec.DoubleValue COLD_BASE_DROP;
    public static final ForgeConfigSpec.DoubleValue COLD_DROP_PER_BLOCK;
    public static final ForgeConfigSpec.DoubleValue COLD_DROP_MAX;

    // Температура игрока
    public static final ForgeConfigSpec.DoubleValue COOL_RATE;
    public static final ForgeConfigSpec.DoubleValue WARM_RATE;
    public static final ForgeConfigSpec.BooleanValue TEMPERATURE_DAMAGE;

    // Метель
    public static final ForgeConfigSpec.BooleanValue BLIZZARD_ENABLED;
    public static final ForgeConfigSpec.DoubleValue BLIZZARD_BASE_CHANCE;

    // Существа / предметы
    public static final ForgeConfigSpec.BooleanValue SPAWN_MASTER;
    public static final ForgeConfigSpec.DoubleValue GIANT_MIN_DAY;
    public static final ForgeConfigSpec.DoubleValue CORE_SLOW_DAYS;
    public static final ForgeConfigSpec.DoubleValue CORE_SLOW_FACTOR;
    public static final ForgeConfigSpec.DoubleValue CORE_MAX_STACK_DAYS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("coldBlock");
        SOURCE_COUNT = b.comment("Сколько блоков холода создать при старте мира")
                .defineInRange("sourceCount", 1, 1, 8);
        MIN_SPAWN_DIST = b.comment("Мин. расстояние от спавна")
                .defineInRange("minSpawnDistance", 10, 1, 1000);
        MAX_SPAWN_DIST = b.comment("Макс. расстояние от спавна")
                .defineInRange("maxSpawnDistance", 100, 2, 2000);
        b.pop();

        b.push("growth");
        START_RADIUS = b.comment("Радиус зоны в первый день (блоки)")
                .defineInRange("startRadius", 8.0, 1.0, 256.0);
        GROWTH_PER_DAY = b.comment("На сколько блоков растёт радиус в первый день")
                .defineInRange("growthPerDay", 6.0, 0.0, 200.0);
        GROWTH_ACCEL = b.comment("Насколько КАЖДЫЙ следующий день рост быстрее (радиус = start + g*d + a*d^2/2)")
                .defineInRange("growthAcceleration", 1.5, 0.0, 100.0);
        MAX_RADIUS = b.comment("Максимальный радиус зоны")
                .defineInRange("maxRadius", 1500.0, 10.0, 30000.0);
        b.pop();

        b.push("snow");
        SNOW_ATTEMPTS_BASE = b.comment("Сколько раз за цикл (4 тика) на игрока пробуем положить снег в первый день")
                .defineInRange("snowAttemptsBase", 6, 1, 1000);
        SNOW_SPEED_GROWTH = b.comment("Во сколько раз скорость снегопада растёт за каждый день (0.3 = +30% в день)")
                .defineInRange("snowSpeedGrowthPerDay", 0.3, 0.0, 10.0);
        SNOW_ATTEMPTS_MAX = b.comment("Потолок попыток (защита от лагов)")
                .defineInRange("snowAttemptsMax", 160, 1, 2000);
        MAX_SNOW_LAYERS = b.comment("Макс. толщина снега в слоях (8 слоёв = 1 блок). 1 слой в день 0, потом +2 слоя в день")
                .defineInRange("maxSnowLayers", 24, 1, 64);
        KILL_PLANTS = b.comment("Растения и посевы в зоне гибнут под снегом")
                .define("killPlants", true);
        b.pop();

        b.push("worldTemperature");
        GLOBAL_COOLING_PER_DAY = b.comment("На сколько °C остывает ВЕСЬ мир за каждый день зимы")
                .defineInRange("globalCoolingPerDay", 0.3, 0.0, 10.0);
        GLOBAL_COOLING_MAX = b.comment("Максимум глобального похолодания, °C")
                .defineInRange("globalCoolingMax", 15.0, 0.0, 100.0);
        COLD_BASE_DROP = b.comment("Падение температуры в центре зоны минимального размера, °C")
                .defineInRange("coldBaseDrop", 12.0, 0.0, 100.0);
        COLD_DROP_PER_BLOCK = b.comment("Дополнительное падение °C на каждый блок радиуса (чем больше зона — тем холоднее)")
                .defineInRange("coldDropPerBlock", 0.25, 0.0, 5.0);
        COLD_DROP_MAX = b.comment("Максимальное падение температуры от блока холода, °C")
                .defineInRange("coldDropMax", 70.0, 0.0, 200.0);
        b.pop();

        b.push("playerTemperature");
        COOL_RATE = b.comment("Макс. скорость остывания тела, °C в секунду")
                .defineInRange("coolRate", 0.05, 0.001, 5.0);
        WARM_RATE = b.comment("Макс. скорость согревания тела, °C в секунду")
                .defineInRange("warmRate", 0.15, 0.001, 5.0);
        TEMPERATURE_DAMAGE = b.comment("Наносить урон от переохлаждения / перегрева")
                .define("temperatureDamage", true);
        b.pop();

        b.push("blizzard");
        BLIZZARD_ENABLED = b.comment("Включить метели (сильный холод + туман на открытой местности)")
                .define("enabled", true);
        BLIZZARD_BASE_CHANCE = b.comment("Шанс метели в первый день; +4% за каждый день зимы (макс. 85%)")
                .defineInRange("baseChancePerDay", 0.2, 0.0, 1.0);
        b.pop();

        b.push("creatures");
        SPAWN_MASTER = b.comment("Создать Мастера по зиме рядом со спавном (один раз за мир)")
                .define("spawnWinterMaster", true);
        GIANT_MIN_DAY = b.comment("С какого дня зимы может появляться Ледяной великан")
                .defineInRange("giantMinDay", 2.0, 0.0, 100.0);
        b.pop();

        b.push("warmthCore");
        CORE_SLOW_DAYS = b.comment("На сколько дней одно Ядро тепла замедляет блок холода")
                .defineInRange("slowDays", 3.0, 0.1, 100.0);
        CORE_SLOW_FACTOR = b.comment("Скорость роста зоны во время замедления (0.25 = в 4 раза медленнее)")
                .defineInRange("slowFactor", 0.25, 0.0, 1.0);
        CORE_MAX_STACK_DAYS = b.comment("Максимум накопленного замедления, дней")
                .defineInRange("maxStackDays", 12.0, 0.1, 365.0);
        b.pop();

        SPEC = b.build();
    }
}
