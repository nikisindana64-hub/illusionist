package com.illusionist.item;

import com.illusionist.entity.MagicianEntity;
import com.illusionist.registry.ModEffects;
import com.illusionist.registry.ModItems;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Монокль Истины. В руке защищает от Гипноза и подмены хотбара.
 * ПКМ: развеивает все иллюзии вокруг, подсвечивает настоящего Фокусника, снимает Гипноз и тьму.
 */
public class MonocleOfTruthItem extends Item {
    public MonocleOfTruthItem(Properties properties) {
        super(properties);
    }

    public static boolean isProtected(Player player) {
        return player.getMainHandItem().is(ModItems.MONOCLE_OF_TRUTH.get())
                || player.getOffhandItem().is(ModItems.MONOCLE_OF_TRUTH.get());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            player.removeEffect(ModEffects.HYPNOSIS);
            player.removeEffect(MobEffects.BLINDNESS);
            player.removeEffect(MobEffects.DARKNESS);

            AABB area = player.getBoundingBox().inflate(28.0D);
            int dispelled = 0;
            List<MagicianEntity> found = serverLevel.getEntitiesOfClass(MagicianEntity.class, area);
            for (MagicianEntity m : found) {
                if (m.isIllusion()) {
                    m.vanishIllusion();
                    dispelled++;
                } else {
                    m.addEffect(new MobEffectInstance(MobEffects.GLOWING, 160, 0, false, false));
                }
            }
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.7F, 1.5F);
            player.displayClientMessage(Component.translatable("message.illusionist.monocle", dispelled), true);
            stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            player.getCooldowns().addCooldown(this, 80);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.illusionist.monocle_of_truth.tooltip1"));
        tooltip.add(Component.translatable("item.illusionist.monocle_of_truth.tooltip2"));
    }
}
