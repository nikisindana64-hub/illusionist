package com.illusionist.registry;

import com.illusionist.IllusionistMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, IllusionistMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.illusionist"))
                    .icon(() -> new ItemStack(ModItems.JOKER_CARD.get()))
                    .displayItems((params, out) -> {
                        out.accept(ModItems.MAGIC_CARD.get());
                        out.accept(ModItems.SMOKE_BOMB.get());
                        out.accept(ModItems.JOKER_CARD.get());
                        out.accept(ModItems.MAGIC_WAND.get());
                        out.accept(ModItems.SHOW_TICKET.get());
                        out.accept(ModItems.TOP_HAT.get());
                        out.accept(ModItems.TRICK_BOX.get());
                        out.accept(ModItems.MONOCLE_OF_TRUTH.get());
                        out.accept(ModItems.CURSED_TOP_HAT_BLOCK.get());
                        out.accept(ModItems.PHANTOM_CURTAIN.get());
                        out.accept(ModItems.CARD_DECK.get());
                        out.accept(ModItems.SILK_SCARF.get());
                        out.accept(ModItems.LUCKY_COIN.get());
                        out.accept(ModItems.CUP_TABLE.get());
                        out.accept(ModItems.CIRCUS_BLOCK.get());
                        out.accept(ModItems.STAGE_VELVET.get());
                        out.accept(ModItems.STAGE_SPOTLIGHT.get());
                        out.accept(ModItems.MAGICIAN_SPAWN_EGG.get());
                        out.accept(ModItems.KIND_MAGICIAN_SPAWN_EGG.get());
                    })
                    .build());
}
