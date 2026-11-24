package com.illusionist.item;

import com.illusionist.entity.KindMagicianEntity;
import com.illusionist.entity.MagicianEntity;
import com.illusionist.util.MagicUtil;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Шёлковый платок: классический фокус «превращение». ПКМ по животному делает из него кролика,
 * по (небоссовому) монстру — курицу. Именованных существ и питомцев не трогает.
 */
public class SilkScarfItem extends Item {
    public SilkScarfItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(player.level() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        if (target instanceof Player || target instanceof MagicianEntity || target instanceof KindMagicianEntity
                || target.hasCustomName() || target instanceof TamableAnimal || target.getMaxHealth() > 100.0F) {
            return InteractionResult.PASS;
        }
        Mob replacement;
        if (target instanceof Monster) {
            replacement = EntityType.CHICKEN.create(level);
        } else if (target instanceof Animal && !(target instanceof net.minecraft.world.entity.animal.Rabbit)) {
            replacement = EntityType.RABBIT.create(level);
        } else {
            return InteractionResult.PASS;
        }
        if (replacement == null) {
            return InteractionResult.PASS;
        }
        replacement.moveTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), 0.0F);
        MagicUtil.poof(level, target.getX(), target.getY() + 0.6D, target.getZ());
        level.playSound(null, target.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.9F, 1.5F);
        target.discard();
        level.addFreshEntity(replacement);
        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        player.getCooldowns().addCooldown(this, 30);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.illusionist.silk_scarf.tooltip1"));
        tooltip.add(Component.translatable("item.illusionist.silk_scarf.tooltip2"));
    }
}
