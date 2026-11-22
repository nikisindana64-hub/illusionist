package com.illusionist.item;

import com.illusionist.director.PerformanceDirector;
import com.illusionist.entity.MagicianEntity;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** «Билет на представление»: следующий акт и немедленный выход настоящего Фокусника. */
public class ShowTicketItem extends Item {
    public ShowTicketItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            MagicianEntity magician = PerformanceDirector.spawnMagicianNear(sp, true);
            if (magician == null) {
                player.displayClientMessage(Component.translatable("message.illusionist.ticket_failed"), true);
                return InteractionResultHolder.fail(stack);
            }
            int next = Math.min(4, PerformanceDirector.getAct(sp) + 1);
            PerformanceDirector.setAct(sp, next);
            PerformanceDirector.announceAct(sp, next);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.illusionist.show_ticket.tooltip"));
        tooltip.add(Component.translatable("item.illusionist.show_ticket.tooltip2"));
    }
}
