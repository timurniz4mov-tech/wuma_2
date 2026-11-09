package com.winterapocalypse.entity;

import com.winterapocalypse.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Мастер по зиме: торговец одеждой, печами и Ядрами тепла. Покупает осколки льда и уголь. */
public class WinterMaster extends AbstractVillager {
    private long lastRestockDay = -10L;

    public WinterMaster(EntityType<? extends WinterMaster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, FrostMonster.class, 14.0F, 0.6D, 0.9D));
        goalSelector.addGoal(1, new PanicGoal(this, 1.2D));
        goalSelector.addGoal(2, new LookAtTradingPlayerGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.35D));
        goalSelector.addGoal(9, new InteractGoal(this, Player.class, 3.0F, 1.0F));
        goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() != Items.VILLAGER_SPAWN_EGG && isAlive() && !isTrading() && !isBaby()) {
            if (hand == InteractionHand.MAIN_HAND) player.awardStat(Stats.TALKED_TO_VILLAGER);
            if (!level().isClientSide) {
                if (!getOffers().isEmpty()) {
                    setTradingPlayer(player);
                    openTradingScreen(player, getDisplayName(), 1);
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        if (offer.shouldRewardExp()) {
            int xp = 3 + random.nextInt(4);
            level().addFreshEntity(new ExperienceOrb(level(), getX(), getY() + 0.5, getZ(), xp));
        }
    }

    private static void add(MerchantOffers o, ItemStack a, ItemStack b, ItemStack r, int uses, int xp) {
        o.add(new MerchantOffer(a, b, r, uses, xp, 0.05F));
    }

    @Override
    protected void updateTrades() {
        MerchantOffers o = getOffers();
        ItemStack none = ItemStack.EMPTY;

        // Он покупает то, чем богата зима
        add(o, new ItemStack(ModItems.FROST_SHARD.get(), 4), none, new ItemStack(Items.EMERALD, 1), 16, 2);
        add(o, new ItemStack(Items.COAL, 16), none, new ItemStack(Items.EMERALD, 1), 16, 1);

        // Зимняя одежда
        add(o, new ItemStack(Items.EMERALD, 5), none, new ItemStack(ModItems.WINTER_HAT.get()), 6, 5);
        add(o, new ItemStack(Items.EMERALD, 9), none, new ItemStack(ModItems.WINTER_JACKET.get()), 6, 8);
        add(o, new ItemStack(Items.EMERALD, 7), none, new ItemStack(ModItems.WINTER_PANTS.get()), 6, 6);
        add(o, new ItemStack(Items.EMERALD, 4), none, new ItemStack(ModItems.WINTER_BOOTS.get()), 6, 4);

        // Печь-обогреватель
        add(o, new ItemStack(Items.EMERALD, 12), none, new ItemStack(ModItems.WINTER_HEATER_ITEM.get()), 4, 10);

        // Парка метели — дорого, нужны осколки / сердце льда
        add(o, new ItemStack(Items.EMERALD, 8), new ItemStack(ModItems.FROST_SHARD.get(), 3),
                new ItemStack(ModItems.BLIZZARD_HOOD.get()), 2, 15);
        add(o, new ItemStack(Items.EMERALD, 18), new ItemStack(ModItems.FROST_HEART.get(), 1),
                new ItemStack(ModItems.BLIZZARD_PARKA.get()), 2, 25);
        add(o, new ItemStack(Items.EMERALD, 14), new ItemStack(ModItems.FROST_SHARD.get(), 4),
                new ItemStack(ModItems.BLIZZARD_LEGGINGS.get()), 2, 20);
        add(o, new ItemStack(Items.EMERALD, 8), new ItemStack(ModItems.FROST_SHARD.get(), 3),
                new ItemStack(ModItems.BLIZZARD_BOOTS.get()), 2, 15);

        // Ядро тепла — замедляет блок холода
        add(o, new ItemStack(Items.EMERALD, 16), new ItemStack(ModItems.FROST_SHARD.get(), 2),
                new ItemStack(ModItems.WARMTH_CORE.get()), 3, 30);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        long day = level().getDayTime() / 24000L;
        if (day >= lastRestockDay + 2) { // товар обновляется раз в 2 дня
            lastRestockDay = day;
            for (MerchantOffer offer : getOffers()) offer.resetUses();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong("LastRestockDay", lastRestockDay);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        lastRestockDay = tag.getLong("LastRestockDay");
    }
}
