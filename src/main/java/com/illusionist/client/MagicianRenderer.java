package com.illusionist.client;

import com.illusionist.IllusionistMod;
import com.illusionist.entity.MagicianEntity;
import com.illusionist.entity.MagicianPose;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.PathfinderMob;

/** Рендерер для злого, одержимого и доброго Фокусника (скин выбирается по варианту). */
public class MagicianRenderer<T extends PathfinderMob & MagicianPose> extends MobRenderer<T, MagicianModel<T>> {
    private static final ResourceLocation[] SKINS = {
            IllusionistMod.id("textures/entity/magician.png"),
            IllusionistMod.id("textures/entity/magician_enraged.png"),
            IllusionistMod.id("textures/entity/magician_kind.png")
    };
    private static final ResourceLocation[] EYES = {
            IllusionistMod.id("textures/entity/magician_eyes.png"),
            IllusionistMod.id("textures/entity/magician_enraged_eyes.png"),
            IllusionistMod.id("textures/entity/magician_kind_eyes.png")
    };

    public MagicianRenderer(EntityRendererProvider.Context context) {
        super(context, new MagicianModel<>(context.bakeLayer(MagicianModel.LAYER)), 0.5F);
        this.addLayer(new GlowLayer<>(this));
    }

    private static int variant(MagicianPose pose) {
        return Math.max(0, Math.min(2, pose.getSkinVariant()));
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return SKINS[variant(entity)];
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        // Тонкая подсказка: у иллюзии нет тени
        boolean illusion = entity instanceof MagicianEntity m && m.isIllusion();
        this.shadowRadius = illusion ? 0.0F : 0.5F;
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** Светящиеся глаза, трещины маски и золото. */
    private static class GlowLayer<T extends PathfinderMob & MagicianPose> extends RenderLayer<T, MagicianModel<T>> {
        GlowLayer(RenderLayerParent<T, MagicianModel<T>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
                           float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                           float netHeadYaw, float headPitch) {
            if (entity.isInvisible()) {
                return;
            }
            VertexConsumer consumer = buffer.getBuffer(RenderType.eyes(EYES[variant(entity)]));
            this.getParentModel().renderToBuffer(poseStack, consumer, 0xF000F0, OverlayTexture.NO_OVERLAY, -1);
        }
    }
}
