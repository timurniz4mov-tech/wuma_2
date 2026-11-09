package com.winterapocalypse.entity;

import com.winterapocalypse.temperature.TemperatureManager;
import com.winterapocalypse.world.ColdManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Общая база зимних монстров: «заряжены холодом» — рядом с ними игрок остывает,
 * удары замедляют и охлаждают, к заморозке они невосприимчивы, а огонь жжёт их сильнее.
 */
public abstract class FrostMonster extends Monster {

    protected FrostMonster(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    /** Радиус холодной ауры (блоки). */
    protected abstract double auraRadius();

    /** Сколько °C в секунду теряет игрок в ауре. */
    protected abstract float auraChill();

    /** Сколько °C теряет игрок от удара. */
    protected abstract float hitChill();

    protected int slownessAmplifier() {
        return 0;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0D));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.FREEZE)) return false;
        if (source.is(DamageTypeTags.IS_FIRE)) amount *= 1.5F;
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity le) {
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, slownessAmplifier()), this);
            if (le instanceof ServerPlayer sp) TemperatureManager.chill(sp, hitChill());
        }
        return hit;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (random.nextInt(4) == 0) {
                level().addParticle(ParticleTypes.SNOWFLAKE, getRandomX(0.7), getRandomY(), getRandomZ(0.7),
                        0.0, -0.02, 0.0);
            }
            return;
        }
        if (tickCount % 20 == 0) {
            double r = auraRadius();
            for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(r))) {
                if (p instanceof ServerPlayer sp && distanceToSqr(sp) <= r * r) {
                    TemperatureManager.chill(sp, auraChill());
                }
            }
        }
    }

    /** Естественное появление: только на снегу и только рядом с зоной блока холода. */
    public static boolean checkSnowSpawn(EntityType<? extends FrostMonster> type, ServerLevelAccessor accessor,
                                         MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (accessor.getDifficulty() == Difficulty.PEACEFUL) return false;
        ServerLevel level = accessor.getLevel();
        BlockState at = level.getBlockState(pos);
        BlockState below = level.getBlockState(pos.below());
        boolean onSnow = at.is(Blocks.SNOW) || below.is(Blocks.SNOW) || below.is(Blocks.SNOW_BLOCK)
                || below.is(Blocks.POWDER_SNOW)
                || (below.hasProperty(BlockStateProperties.SNOWY) && below.getValue(BlockStateProperties.SNOWY));
        if (!onSnow) return false;
        return ColdManager.isInZone(level, pos, 16.0);
    }
}
