package com.winterapocalypse;

import com.winterapocalypse.item.WarmthCoreItem;
import com.winterapocalypse.item.WinterArmorItem;
import com.winterapocalypse.item.WinterArmorMaterial;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, WinterApocalypse.MODID);

    // ---- блоки
    public static final RegistryObject<Item> COLD_BLOCK_ITEM = ITEMS.register("cold_block",
            () -> new BlockItem(ModBlocks.COLD_BLOCK.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> WINTER_HEATER_ITEM = ITEMS.register("winter_heater",
            () -> new BlockItem(ModBlocks.WINTER_HEATER.get(), new Item.Properties()));

    // ---- материалы и особые предметы
    public static final RegistryObject<Item> FROST_SHARD = ITEMS.register("frost_shard",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> FROST_HEART = ITEMS.register("frost_heart",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> WARMTH_CORE = ITEMS.register("warmth_core",
            () -> new WarmthCoreItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(16)));

    // ---- Зимняя одежда (лучше кожаной): +6 °C за часть, 10% защиты от метели за часть
    public static final RegistryObject<Item> WINTER_HAT = ITEMS.register("winter_hat",
            () -> new WinterArmorItem(WinterArmorMaterial.WINTER, ArmorItem.Type.HELMET, new Item.Properties(), 6.0, 0.10));
    public static final RegistryObject<Item> WINTER_JACKET = ITEMS.register("winter_jacket",
            () -> new WinterArmorItem(WinterArmorMaterial.WINTER, ArmorItem.Type.CHESTPLATE, new Item.Properties(), 6.0, 0.10));
    public static final RegistryObject<Item> WINTER_PANTS = ITEMS.register("winter_pants",
            () -> new WinterArmorItem(WinterArmorMaterial.WINTER, ArmorItem.Type.LEGGINGS, new Item.Properties(), 6.0, 0.10));
    public static final RegistryObject<Item> WINTER_BOOTS = ITEMS.register("winter_boots",
            () -> new WinterArmorItem(WinterArmorMaterial.WINTER, ArmorItem.Type.BOOTS, new Item.Properties(), 6.0, 0.10));

    // ---- Парка метели (лучшая): +8 °C за часть, 25% защиты от метели за часть (полный комплект = 100%)
    public static final RegistryObject<Item> BLIZZARD_HOOD = ITEMS.register("blizzard_hood",
            () -> new WinterArmorItem(WinterArmorMaterial.BLIZZARD, ArmorItem.Type.HELMET, new Item.Properties().rarity(Rarity.RARE), 8.0, 0.25));
    public static final RegistryObject<Item> BLIZZARD_PARKA = ITEMS.register("blizzard_parka",
            () -> new WinterArmorItem(WinterArmorMaterial.BLIZZARD, ArmorItem.Type.CHESTPLATE, new Item.Properties().rarity(Rarity.RARE), 8.0, 0.25));
    public static final RegistryObject<Item> BLIZZARD_LEGGINGS = ITEMS.register("blizzard_leggings",
            () -> new WinterArmorItem(WinterArmorMaterial.BLIZZARD, ArmorItem.Type.LEGGINGS, new Item.Properties().rarity(Rarity.RARE), 8.0, 0.25));
    public static final RegistryObject<Item> BLIZZARD_BOOTS = ITEMS.register("blizzard_boots",
            () -> new WinterArmorItem(WinterArmorMaterial.BLIZZARD, ArmorItem.Type.BOOTS, new Item.Properties().rarity(Rarity.RARE), 8.0, 0.25));

    // ---- яйца призыва
    public static final RegistryObject<Item> FROST_WALKER_EGG = ITEMS.register("frost_walker_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.FROST_WALKER, 0x9CC8E0, 0x2E4A6B, new Item.Properties()));
    public static final RegistryObject<Item> FROST_GIANT_EGG = ITEMS.register("frost_giant_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.FROST_GIANT, 0xD8F0FF, 0x3A7BC8, new Item.Properties()));
    public static final RegistryObject<Item> WINTER_MASTER_EGG = ITEMS.register("winter_master_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.WINTER_MASTER, 0x5A7FA8, 0xF0F4FA, new Item.Properties()));
}
