package com.illusionist.registry;

import com.illusionist.IllusionistMod;
import com.illusionist.entity.MagicianEntity;
import com.illusionist.item.JokerCardItem;
import com.illusionist.item.CardDeckItem;
import com.illusionist.item.LuckyCoinItem;
import com.illusionist.item.SilkScarfItem;
import com.illusionist.item.MagicCardItem;
import com.illusionist.item.MagicWandItem;
import com.illusionist.item.MonocleOfTruthItem;
import com.illusionist.item.ShowTicketItem;
import com.illusionist.item.SmokeBombItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(IllusionistMod.MODID);

    public static final DeferredItem<MagicCardItem> MAGIC_CARD = ITEMS.register("magic_card",
            () -> new MagicCardItem(new Item.Properties()));

    public static final DeferredItem<CardDeckItem> CARD_DECK = ITEMS.register("card_deck",
            () -> new CardDeckItem(new Item.Properties().durability(40).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<SilkScarfItem> SILK_SCARF = ITEMS.register("silk_scarf",
            () -> new SilkScarfItem(new Item.Properties().durability(24).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<LuckyCoinItem> LUCKY_COIN = ITEMS.register("lucky_coin",
            () -> new LuckyCoinItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final DeferredItem<net.minecraft.world.item.BlockItem> CUP_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.CUP_TABLE);

    public static final DeferredItem<net.minecraft.world.item.BlockItem> CIRCUS_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.CIRCUS_BLOCK);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> STAGE_VELVET = ITEMS.registerSimpleBlockItem(ModBlocks.STAGE_VELVET);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> STAGE_SPOTLIGHT = ITEMS.registerSimpleBlockItem(ModBlocks.STAGE_SPOTLIGHT);

    public static final DeferredItem<SmokeBombItem> SMOKE_BOMB = ITEMS.register("smoke_bomb",
            () -> new SmokeBombItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<JokerCardItem> JOKER_CARD = ITEMS.register("joker_card",
            () -> new JokerCardItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<MagicWandItem> MAGIC_WAND = ITEMS.register("magic_wand",
            () -> new MagicWandItem(new Item.Properties().durability(64).rarity(Rarity.RARE)));

    public static final DeferredItem<ShowTicketItem> SHOW_TICKET = ITEMS.register("show_ticket",
            () -> new ShowTicketItem(new Item.Properties().stacksTo(8).rarity(Rarity.EPIC)));

    public static final DeferredItem<ArmorItem> TOP_HAT = ITEMS.register("top_hat",
            () -> new ArmorItem(ModArmorMaterials.TOP_HAT, ArmorItem.Type.HELMET,
                    new Item.Properties()
                            .durability(ArmorItem.Type.HELMET.getDurability(15))
                            .rarity(Rarity.EPIC)));

    public static final DeferredItem<MonocleOfTruthItem> MONOCLE_OF_TRUTH = ITEMS.register("monocle_of_truth",
            () -> new MonocleOfTruthItem(new Item.Properties().durability(48).rarity(Rarity.RARE)));

    public static final DeferredItem<net.minecraft.world.item.BlockItem> CURSED_TOP_HAT_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.CURSED_TOP_HAT);

    public static final DeferredItem<net.minecraft.world.item.BlockItem> PHANTOM_CURTAIN = ITEMS.registerSimpleBlockItem(ModBlocks.PHANTOM_CURTAIN);

    public static final DeferredItem<DeferredSpawnEggItem> KIND_MAGICIAN_SPAWN_EGG = ITEMS.register("kind_magician_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.KIND_MAGICIAN, 0x22307A, 0xF4C840, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> MAGICIAN_SPAWN_EGG = ITEMS.register("magician_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.MAGICIAN, 0x15121A, 0xA01020, new Item.Properties()));

    public static final DeferredItem<net.minecraft.world.item.BlockItem> TRICK_BOX = ITEMS.registerSimpleBlockItem(ModBlocks.TRICK_BOX);
}
