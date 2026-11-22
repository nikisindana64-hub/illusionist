package com.illusionist.item;

import com.illusionist.entity.MagicianEntity;
import com.illusionist.registry.ModEffects;
import com.illusionist.registry.ModItems;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** «Выбери карту»: азартная игра. Может подарить силу, а может позвать хозяина. */
public class JokerCardItem extends Item {
    public JokerCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            playCard(serverLevel, player);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private void playCard(ServerLevel level, Player player) {
        RandomSource random = level.getRandom();
        level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.2F);
        int roll = random.nextInt(100);
        String key;
        if (roll < 16) {
            key = "hearts";
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300, 1));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1200, 1));
        } else if (roll < 32) {
            key = "diamonds";
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 0));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 0));
        } else if (roll < 48) {
            key = "clubs";
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 1));
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 3600, 0));
        } else if (roll < 58) {
            key = "trick";
            giveOrDrop(player, new ItemStack(ModItems.SMOKE_BOMB.get(), 2));
            giveOrDrop(player, new ItemStack(ModItems.MAGIC_CARD.get(), 8));
        } else if (roll < 80) {
            key = "spades";
            player.addEffect(new MobEffectInstance(ModEffects.STAGE_FRIGHT, 600, 0));
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160, 0));
        } else if (roll < 92) {
            key = "jester";
            player.addEffect(new MobEffectInstance(ModEffects.STAGE_FRIGHT, 300, 0));
            for (int i = 0; i < 2; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                MagicianEntity.spawnIllusion(level,
                        player.getX() + Math.cos(angle) * 6, player.getY(), player.getZ() + Math.sin(angle) * 6, player);
            }
        } else {
            key = "magician";
            MagicianEntity.summonNear(level, player);
        }
        player.displayClientMessage(Component.translatable("message.illusionist.card." + key), true);
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.illusionist.joker_card.tooltip"));
    }
}
