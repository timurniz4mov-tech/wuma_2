package com.winterapocalypse.item;

import com.winterapocalypse.ModItems;
import com.winterapocalypse.WinterApocalypse;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

/** Защита: {шлем, нагрудник, поножи, ботинки}. Текстуры: textures/models/armor/<name>_layer_1/2.png */
public enum WinterArmorMaterial implements ArmorMaterial {
    // Зимняя одежда: лучше кожаной (1/3/2/1), хуже парки
    WINTER("winter", 15, new int[]{2, 4, 3, 1}, 12, () -> Ingredient.of(Items.WHITE_WOOL), 0.0F, 0.0F),
    // Парка метели: лучшая
    BLIZZARD("blizzard", 26, new int[]{2, 5, 4, 2}, 16, () -> Ingredient.of(ModItems.FROST_SHARD.get()), 1.0F, 0.05F);

    private final String name;
    private final int durabilityMultiplier;
    private final int[] defense;
    private final int enchantability;
    private final Supplier<Ingredient> repair;
    private final float toughness;
    private final float knockbackResistance;

    WinterArmorMaterial(String name, int durabilityMultiplier, int[] defense, int enchantability,
                        Supplier<Ingredient> repair, float toughness, float knockbackResistance) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.defense = defense;
        this.enchantability = enchantability;
        this.repair = repair;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
    }

    private static int baseDurability(ArmorItem.Type type) {
        return switch (type) {
            case BOOTS -> 13;
            case LEGGINGS -> 15;
            case CHESTPLATE -> 16;
            case HELMET -> 11;
        };
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return baseDurability(type) * durabilityMultiplier;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> defense[0];
            case CHESTPLATE -> defense[1];
            case LEGGINGS -> defense[2];
            case BOOTS -> defense[3];
        };
    }

    @Override
    public int getEnchantmentValue() {
        return enchantability;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return repair.get();
    }

    @Override
    public String getName() {
        return WinterApocalypse.MODID + ":" + name;
    }

    @Override
    public float getToughness() {
        return toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return knockbackResistance;
    }
}
