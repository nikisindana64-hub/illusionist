package com.illusionist.item;

import com.illusionist.entity.CardProjectile;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import java.util.List;

/** Колода карт: веер из пяти метательных карт за одно использование. */
public class CardDeckItem extends Item {
    public CardDeckItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.4F);
        if (!level.isClientSide) {
            for (int i = -2; i <= 2; i++) {
                CardProjectile card = new CardProjectile(level, player);
                card.shootFromRotation(player, player.getXRot(), player.getYRot() + i * 9.0F, 0.0F, 1.8F, 1.0F);
                level.addFreshEntity(card);
            }
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        player.getCooldowns().addCooldown(this, 30);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.illusionist.card_deck.tooltip"));
    }
}
