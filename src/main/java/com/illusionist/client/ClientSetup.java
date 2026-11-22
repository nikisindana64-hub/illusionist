package com.illusionist.client;

import com.illusionist.IllusionistMod;
import com.illusionist.entity.KindMagicianEntity;
import com.illusionist.entity.MagicianEntity;
import com.illusionist.registry.ModEntities;
import com.illusionist.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = IllusionistMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MagicianModel.LAYER, MagicianModel::createBodyLayer);
        event.registerLayerDefinition(TopHatArmorModel.LAYER, TopHatArmorModel::createLayer);
    }

    /** Объёмная модель цилиндра на голове вместо плоского шлема. */
    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private TopHatArmorModel model;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot,
                                               HumanoidModel<?> original) {
                if (model == null) {
                    model = new TopHatArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(TopHatArmorModel.LAYER));
                }
                @SuppressWarnings({"rawtypes", "unchecked"})
                HumanoidModel<LivingEntity> raw = (HumanoidModel) original;
                raw.copyPropertiesTo(model);
                model.head.visible = slot == EquipmentSlot.HEAD;
                return model;
            }
        }, ModItems.TOP_HAT.get());
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MAGICIAN.get(), ctx -> new MagicianRenderer<MagicianEntity>(ctx));
        event.registerEntityRenderer(ModEntities.KIND_MAGICIAN.get(), ctx -> new MagicianRenderer<KindMagicianEntity>(ctx));
        event.registerEntityRenderer(ModEntities.CARD.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.SMOKE_BOMB.get(), ThrownItemRenderer::new);
    }
}
