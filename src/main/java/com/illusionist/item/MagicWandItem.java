package com.illusionist.item;

import com.illusionist.entity.MagicianEntity;
import com.illusionist.util.MagicUtil;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Волшебная палочка.
 *  ПКМ — «Исчезновение»: короткий телепорт вперёд.
 *  Shift+ПКМ — «Разоблачение»: рассеивает иллюзии вокруг и подсвечивает настоящего Фокусника.
 */
public class MagicWandItem extends Item {
    public MagicWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        boolean ok = player.isShiftKeyDown() ? reveal(serverLevel, player) : blink(serverLevel, player);
        if (!ok) {
            return InteractionResultHolder.fail(stack);
        }
        stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        player.getCooldowns().addCooldown(this, player.isShiftKeyDown() ? 160 : 60);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    private boolean blink(ServerLevel level, Player player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(10.0));
        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        Vec3 dest = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation().subtract(look.scale(0.7));
        MagicUtil.poof(level, player.getX(), player.getY() + 1, player.getZ());
        double footY = dest.y - player.getEyeHeight();
        if (!player.randomTeleport(dest.x, footY, dest.z, true)) {
            player.displayClientMessage(Component.translatable("message.illusionist.wand_blocked"), true);
            return false;
        }
        player.fallDistance = 0.0F;
        MagicUtil.poof(level, player.getX(), player.getY() + 1, player.getZ());
        level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    private boolean reveal(ServerLevel level, Player player) {
        AABB area = player.getBoundingBox().inflate(24.0);
        List<MagicianEntity> found = level.getEntitiesOfClass(MagicianEntity.class, area);
        for (MagicianEntity magician : found) {
            if (magician.isIllusion()) {
                magician.vanishIllusion();
            } else {
                magician.addEffect(new MobEffectInstance(MobEffects.GLOWING, 140, 0, false, false));
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, SoundSource.PLAYERS, 1.0F, 1.4F);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                player.getX(), player.getY() + 1.2, player.getZ(), 40, 1.0, 1.0, 1.0, 0.5);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.illusionist.magic_wand.tooltip1"));
        tooltip.add(Component.translatable("item.illusionist.magic_wand.tooltip2"));
    }
}
