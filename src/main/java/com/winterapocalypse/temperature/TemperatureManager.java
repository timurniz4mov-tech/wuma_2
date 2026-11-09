package com.winterapocalypse.temperature;

import com.winterapocalypse.WinterConfig;
import com.winterapocalypse.item.WinterArmorItem;
import com.winterapocalypse.network.ModNetwork;
import com.winterapocalypse.network.TempPacket;
import com.winterapocalypse.world.ColdManager;
import com.winterapocalypse.world.ColdWorldData;
import com.winterapocalypse.world.HeaterRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.network.PacketDistributor;

/**
 * Система температуры.
 *  - Температура МИРА в точке: биом + время суток + высота + дождь + метель + глобальное похолодание + блоки холода.
 *  - Температура ИГРОКА (тела): стремится к значению, зависящему от воздуха, одежды, ветра метели и тепла рядом.
 */
public final class TemperatureManager {
    public static final float NORMAL = 36.6f;
    private static final String TAG = "PlayerPersisted"; // эта ветка NBT сохраняется после смерти
    private static final String KEY_BODY = "wa_body";

    private TemperatureManager() {}

    // ------------------------------------------------------------------ данные игрока

    public static CompoundTag persisted(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(TAG, 10)) root.put(TAG, new CompoundTag());
        return root.getCompound(TAG);
    }

    public static float getBody(Player p) {
        CompoundTag d = persisted(p);
        return d.contains(KEY_BODY) ? d.getFloat(KEY_BODY) : NORMAL;
    }

    public static void reset(ServerPlayer p) {
        persisted(p).putFloat(KEY_BODY, NORMAL);
        p.setTicksFrozen(0);
    }

    /** Мгновенно охладить тело (удары и аура ледяных монстров). */
    public static void chill(ServerPlayer p, float amount) {
        if (p.isCreative() || p.isSpectator()) return;
        CompoundTag d = persisted(p);
        float b = d.contains(KEY_BODY) ? d.getFloat(KEY_BODY) : NORMAL;
        d.putFloat(KEY_BODY, Math.max(20.0f, b - amount));
    }

    // ------------------------------------------------------------------ температура мира

    /** Температура воздуха в °C в точке мира. */
    public static double getAmbient(ServerLevel level, BlockPos pos) {
        if (level.dimension() == Level.NETHER) return 30.0;
        if (level.dimension() == Level.END) return 8.0;

        ServerLevel overworld = level.getServer().overworld();
        ColdWorldData data = ColdWorldData.get(overworld);
        long dayTime = overworld.getDayTime();

        // 1. Биом: 0.8 (равнины) ~ 18°C, 2.0 (пустыня) ~ 48°C, 0.0 (снежные) ~ -2°C
        double t = level.getBiome(pos).value().getBaseTemperature() * 25.0 - 2.0;

        boolean sky = level.canSeeSky(pos);

        if (sky) {
            // 2. День/ночь: ±4°C (максимум в полдень)
            double f = Math.cos(((level.getDayTime() % 24000L) - 6000.0) / 24000.0 * Math.PI * 2.0);
            t += f * 4.0;
            // 3. Дождь
            if (level.isRainingAt(pos)) t -= 3.0;
        } else if (pos.getY() < 55) {
            // пещеры: ближе к постоянным 12°C
            t = t * 0.4 + 12.0 * 0.6;
        } else {
            t += 3.0; // под крышей чуть теплее
        }

        // 4. Высота
        if (pos.getY() > 90) t -= (pos.getY() - 90) * 0.06;

        // 5. Глобальное похолодание мира с каждым днём
        t -= Math.min(WinterConfig.GLOBAL_COOLING_MAX.get(),
                ColdManager.globalAge(data, dayTime) * WinterConfig.GLOBAL_COOLING_PER_DAY.get());

        // 6. Блоки холода (берём самый сильный). Под землёй/крышей слабее.
        double best = 0.0;
        for (ColdWorldData.Source s : data.sources) {
            best = Math.max(best, ColdManager.sourceDrop(s, dayTime, pos.getX() + 0.5, pos.getZ() + 0.5));
        }
        t -= sky ? best : best * 0.7;

        // 7. Метель: воздух холоднее ещё на 4°C (только на открытом месте и когда и так прохладно)
        if (sky && t < 3.0) {
            t -= 4.0 * ColdManager.blizzardIntensity(overworld, data);
        }

        return Math.max(-100.0, Math.min(60.0, t));
    }

    /** Насколько игрок «под ветром» метели, 0..1 (на открытом месте, когда холодно). */
    public static double blizzardExposure(ServerLevel level, BlockPos pos, double ambient) {
        if (level.dimension() != Level.OVERWORLD) return 0.0;
        if (!level.canSeeSky(pos) || ambient >= 3.0) return 0.0;
        ServerLevel ow = level.getServer().overworld();
        return ColdManager.blizzardIntensity(ow, ColdWorldData.get(ow));
    }

    // ------------------------------------------------------------------ факторы игрока

    /** Тепло от одежды: зимняя +6, парка +8 за часть; кожаная 4, остальная броня 2. */
    public static double insulation(Player p) {
        double sum = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) continue;
            ItemStack st = p.getItemBySlot(slot);
            if (st.getItem() instanceof WinterArmorItem w) {
                sum += w.warmth;
            } else if (st.getItem() instanceof ArmorItem armor) {
                sum += armor.getMaterial() == ArmorMaterials.LEATHER ? 4.0 : 2.0;
            }
        }
        return sum;
    }

    /** Защита от ветра метели 0..1 (полный комплект парки = 1.0). */
    public static double windProtection(Player p) {
        double sum = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) continue;
            if (p.getItemBySlot(slot).getItem() instanceof WinterArmorItem w) sum += w.windProtection;
        }
        return Math.min(1.0, sum);
    }

    private static double heatValue(BlockState s) {
        if (s.isAir()) return 0;
        if (s.is(Blocks.FIRE) || s.is(Blocks.SOUL_FIRE)) return 12;
        if (s.is(Blocks.CAMPFIRE) || s.is(Blocks.SOUL_CAMPFIRE)) return s.getValue(BlockStateProperties.LIT) ? 14 : 0;
        if (s.getFluidState().is(FluidTags.LAVA)) return 16;
        if (s.is(Blocks.MAGMA_BLOCK)) return 5;
        if (s.is(Blocks.FURNACE) || s.is(Blocks.BLAST_FURNACE) || s.is(Blocks.SMOKER)) {
            return s.getValue(BlockStateProperties.LIT) ? 9 : 0;
        }
        if (s.is(Blocks.TORCH) || s.is(Blocks.WALL_TORCH) || s.is(Blocks.LANTERN)) return 2;
        return 0;
    }

    /** Тепло от костров, печей, лавы и факелов рядом (радиус ~5 блоков). */
    private static double heatNear(Player p) {
        BlockPos c = p.blockPosition();
        double sum = 0;
        for (BlockPos bp : BlockPos.betweenClosed(c.offset(-3, -2, -3), c.offset(3, 2, 3))) {
            double h = heatValue(p.level().getBlockState(bp));
            if (h <= 0) continue;
            double dist = Math.sqrt(bp.distSqr(c));
            sum += h * Math.max(0.0, 1.0 - dist / 5.0);
        }
        return Math.min(sum, 30.0);
    }

    // ------------------------------------------------------------------ главный тик игрока

    public static void tickPlayer(ServerPlayer p) {
        CompoundTag d = persisted(p);
        if (!d.contains(KEY_BODY)) d.putFloat(KEY_BODY, NORMAL);
        float body = d.getFloat(KEY_BODY);
        ServerLevel level = p.serverLevel();
        boolean immune = p.isCreative() || p.isSpectator();

        if (p.tickCount % 20 == 0) {
            BlockPos pos = p.blockPosition();
            double ambient = getAmbient(level, pos);
            double exposure = blizzardExposure(level, pos, ambient);
            double prot = windProtection(p);

            if (immune || !p.isAlive()) {
                body = NORMAL;
            } else {
                double heat = Math.min(45.0, heatNear(p) + HeaterRegistry.heatAt(level, pos));
                double eff = ambient + insulation(p) + heat;
                eff -= 14.0 * exposure * (1.0 - prot); // ветер метели — парка защищает
                if (p.isInWater()) eff -= 10.0;
                if (p.isOnFire()) eff += 20.0;

                double target;
                if (eff < 15.0) target = NORMAL - (15.0 - eff) * 0.12;       // холодно
                else if (eff > 28.0) target = NORMAL + (eff - 28.0) * 0.08;   // жарко
                else target = NORMAL;
                target = Math.max(20.0, Math.min(42.0, target));

                float diff = (float) (target - body);
                float rate = diff < 0 ? WinterConfig.COOL_RATE.get().floatValue() : WinterConfig.WARM_RATE.get().floatValue();
                body += Math.max(-rate, Math.min(rate, diff));

                applyEffects(p, body);
            }
            d.putFloat(KEY_BODY, body);

            ServerLevel ow = level.getServer().overworld();
            float day = (float) ColdManager.globalAge(ColdWorldData.get(ow), ow.getDayTime());
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                    new TempPacket(body, (float) ambient, day, (float) exposure, (float) prot));
        }

        // эффект инея на экране (ванильная «заморозка»), без ванильного урона
        if (!immune && body < 35.0f && p.isAlive()) {
            float frac = Math.min(1.0f, (35.0f - body) / 5.0f);
            int target = (int) (frac * (p.getTicksRequiredToFreeze() - 2));
            if (p.getTicksFrozen() < target) p.setTicksFrozen(target);
        }
    }

    private static void applyEffects(ServerPlayer p, float body) {
        int sec = p.tickCount / 20;
        boolean dmg = WinterConfig.TEMPERATURE_DAMAGE.get();

        if (body < 35.0f) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, body < 33.5f ? 1 : 0, true, false));
            p.causeFoodExhaustion(0.1f); // на холоде быстрее хочется есть
        }
        if (body < 33.0f) {
            p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 0, true, false));
        }
        if (dmg && body < 32.0f) {
            if (body < 30.0f) {
                if (sec % 2 == 0) p.hurt(p.damageSources().freeze(), 2.0f);
            } else if (sec % 4 == 0) {
                p.hurt(p.damageSources().freeze(), 1.0f);
            }
        }

        if (body > 38.5f) {
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, false));
        }
        if (dmg && body > 39.5f && sec % 4 == 0) {
            p.hurt(p.damageSources().generic(), 1.0f);
        }
    }
}
