package com.winterapocalypse.entity;

import com.winterapocalypse.temperature.TemperatureManager;
import com.winterapocalypse.WinterConfig;
import com.winterapocalypse.world.ColdManager;
import com.winterapocalypse.world.ColdWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/** Ледяной великан: 4.2 блока ростом. Топает по земле — ударная волна холода. */
public class FrostGiant extends FrostMonster {
    private int stompCooldown = 60;

    public FrostGiant(EntityType<? extends FrostGiant> type, Level level) {
        super(type, level);
        this.xpReward = 40;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 140.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.26D)
                .add(Attributes.ATTACK_DAMAGE, 14.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.ARMOR, 6.0D);
    }

    @Override protected double auraRadius() { return 9.0D; }
    @Override protected float auraChill() { return 0.35F; }
    @Override protected float hitChill() { return 2.0F; }
    @Override protected int slownessAmplifier() { return 1; }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        if (stompCooldown > 0) stompCooldown--;
        LivingEntity t = getTarget();
        if (t != null && stompCooldown <= 0 && onGround() && distanceToSqr(t) < 36.0D) {
            stomp();
            stompCooldown = 100 + random.nextInt(60);
        }
    }

    private void stomp() {
        ServerLevel sl = (ServerLevel) level();
        sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.5F, 0.6F);
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE, getX(), getY() + 0.2, getZ(),
                100, 3.5, 0.3, 3.5, 0.1);
        sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.2, getZ(), 40, 3.0, 0.2, 3.0, 0.05);
        for (Player p : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(6.0, 1.5, 6.0))) {
            if (p.isCreative() || p.isSpectator()) continue;
            p.hurt(damageSources().mobAttack(this), 7.0F);
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1), this);
            p.setDeltaMovement(p.getDeltaMovement().add(0.0, 0.5, 0.0));
            p.hurtMarked = true;
            if (p instanceof ServerPlayer sp) TemperatureManager.chill(sp, 1.5F);
        }
    }

    /** Появляется только после N-го дня зимы. */
    public static boolean checkGiantSpawn(EntityType<? extends FrostGiant> type, ServerLevelAccessor accessor,
                                          MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (!checkSnowSpawn(type, accessor, reason, pos, random)) return false;
        ServerLevel level = accessor.getLevel();
        ColdWorldData data = ColdWorldData.get(level);
        return ColdManager.globalAge(data, level.getDayTime()) >= WinterConfig.GIANT_MIN_DAY.get();
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.RAVAGER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return SoundEvents.IRON_GOLEM_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.IRON_GOLEM_DEATH; }
    @Override protected void playStepSound(BlockPos pos, BlockState state) { playSound(SoundEvents.IRON_GOLEM_STEP, 1.0F, 0.7F); }
}
