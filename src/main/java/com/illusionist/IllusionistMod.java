package com.illusionist;

import com.illusionist.registry.ModArmorMaterials;
import com.illusionist.registry.ModBlocks;
import com.illusionist.registry.ModCreativeTabs;
import com.illusionist.registry.ModEffects;
import com.illusionist.registry.ModEntities;
import com.illusionist.registry.ModItems;
import com.illusionist.registry.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(IllusionistMod.MODID)
public class IllusionistMod {
    public static final String MODID = "illusionist";

    public IllusionistMod(IEventBus modBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModArmorMaterials.ARMOR_MATERIALS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
