package com.illusionist.item;

import com.illusionist.registry.ModEffects;
import com.illusionist.util.Screamer;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Монета фокусника: подбросить. Орёл — удача и регенерация, решка — невезение, слабость и страх. */
public class LuckyCoinItem extends Item {
    public LuckyCoinItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            boolean heads = level.random.nextBoolean();
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARMOR_EQUIP_GOLD.value(), SoundSource.PLAYERS, 1.0F, 1.8F);
            if (heads) {
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 6000, 0));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0));
                player.displayClientMessage(Component.translatable("item.illusionist.lucky_coin.heads"), true);
            } else {
                player.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 3000, 0));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
                player.addEffect(new MobEffectInstance(ModEffects.STAGE_FRIGHT, 200, 0));
                player.displayClientMessage(Component.translatable("item.illusionist.lucky_coin.tails"), true);
                if (player instanceof ServerPlayer sp && level.random.nextInt(5) == 0) {
                    Screamer.trigger(sp);
                }
            }
        }
        player.getCooldowns().addCooldown(this, 100);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.illusionist.lucky_coin.tooltip"));
    }
}
