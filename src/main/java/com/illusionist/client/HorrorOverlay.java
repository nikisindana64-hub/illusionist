package com.illusionist.client;

import com.illusionist.IllusionistMod;
import com.illusionist.registry.ModEffects;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

/**
 * Пульсирующая багровая виньетка театрального занавеса по краям экрана
 * при «Сценическом страхе» и «Гипнозе». Рисуется один раз за кадр.
 */
@EventBusSubscriber(modid = IllusionistMod.MODID, value = Dist.CLIENT)
public class HorrorOverlay {
    private static final ResourceLocation LAYER = ResourceLocation.withDefaultNamespace("camera_overlays");

    private static final ResourceLocation TOP_LAYER = ResourceLocation.withDefaultNamespace("subtitle_overlay");
    private static final ResourceLocation FACE = IllusionistMod.id("textures/gui/screamer_face.png");

    /** Скример: лицо на весь экран, пока активен эффект JUMPSCARE (рисуется поверх всего интерфейса). */
    @SubscribeEvent
    public static void onRenderJumpscare(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(TOP_LAYER)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }
        MobEffectInstance inst = player.getEffect(ModEffects.JUMPSCARE);
        if (inst == null) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        float alpha = Math.min(1.0F, inst.getDuration() / 5.0F);
        int shake = inst.getDuration() % 2 == 0 ? 3 : -3;
        g.fill(0, 0, w, h, ((int) (alpha * 235) << 24) | 0x110000);
        int size = Math.max(w, h);
        int x = (w - size) / 2 + shake;
        int y = (h - size) / 2 - shake;
        RenderSystem.enableBlend();
        g.setColor(1.0F, 1.0F, 1.0F, alpha);
        g.blit(FACE, x, y, size, size, 0.0F, 0.0F, 256, 256, 256, 256);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(LAYER)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) {
            return;
        }
        boolean fright = player.hasEffect(ModEffects.STAGE_FRIGHT);
        boolean hypno = player.hasEffect(ModEffects.HYPNOSIS);
        if (!fright && !hypno) {
            return;
        }

        GuiGraphics g = event.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        float pulse = (float) (0.55 + 0.35 * Math.sin(System.currentTimeMillis() / 220.0));
        int alpha = (int) ((hypno ? 120 : 85) * pulse);
        int edge = (alpha << 24) | (hypno ? 0x5A0622 : 0x3A0410);
        int clear = 0x00000000;

        g.fillGradient(0, 0, w, h / 5, edge, clear);
        g.fillGradient(0, h - h / 5, w, h, clear, edge);
        int side = w / 12;
        g.fill(0, 0, side, h, edge);                   // кулисы по бокам
        g.fill(w - side, 0, w, h, edge);
    }
}
