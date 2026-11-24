package com.illusionist.director;

import com.illusionist.IllusionistMod;
import com.illusionist.entity.MagicianEntity;
import com.illusionist.item.MonocleOfTruthItem;
import com.illusionist.registry.ModEffects;
import com.illusionist.registry.ModEntities;
import com.illusionist.registry.ModSounds;
import com.illusionist.util.MagicUtil;
import com.illusionist.util.Screamer;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * «Двойник среди жителей». В деревне рядом с настоящим жителем появляется его копия — это Фокусник.
 * Она не ходит и не торгует, только поворачивается вслед за игроком (слежение). Раскрывается, если
 * подойти вплотную, ударить его или нажать ПКМ. Монокль Истины в руке подсвечивает самозванца.
 */
@EventBusSubscriber(modid = IllusionistMod.MODID)
public class ImpostorHandler {
    public static final String TAG = "illusionist_impostor";
    private static final String TAG_NEAR = "illusionist_imp_near";

    public static boolean isImpostor(Entity e) {
        return e instanceof Villager && e.getPersistentData().getBoolean(TAG);
    }

    /** Подсаживает двойника к случайному жителю рядом. Одновременно не больше одного. */
    public static boolean spawnImpostor(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!level.getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(64.0D), ImpostorHandler::isImpostor).isEmpty()) {
            return false;
        }
        List<Villager> list = level.getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(40.0D),
                v -> !v.isBaby() && !isImpostor(v));
        if (list.isEmpty()) {
            return false;
        }
        Villager src = list.get(level.random.nextInt(list.size()));
        for (int attempt = 0; attempt < 8; attempt++) {
            double ox = (level.random.nextDouble() - 0.5D) * 7.0D;
            double oz = (level.random.nextDouble() - 0.5D) * 7.0D;
            if (Math.abs(ox) < 1.5D && Math.abs(oz) < 1.5D) {
                continue;
            }
            BlockPos spot = MagicUtil.findStandable(level, src.getX() + ox, src.getY(), src.getZ() + oz);
            if (spot == null) {
                continue;
            }
            Villager fake = EntityType.VILLAGER.create(level);
            if (fake == null) {
                return false;
            }
            fake.setVillagerData(src.getVillagerData());
            fake.moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, src.getYRot(), 0.0F);
            fake.getPersistentData().putBoolean(TAG, true);
            fake.setNoAi(true);
            fake.setSilent(true);
            fake.setPersistenceRequired();
            level.addFreshEntity(fake);
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator() || player.tickCount % 2 != 0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        List<Villager> list = level.getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(24.0D),
                ImpostorHandler::isImpostor);
        for (Villager v : list) {
            lookAt(v, player);
            double dist = v.distanceTo(player);
            if (MonocleOfTruthItem.isProtected(player) && dist < 16.0D) {
                v.addEffect(new MobEffectInstance(MobEffects.GLOWING, 12, 0, false, false));
            }
            CompoundTag tag = v.getPersistentData();
            int near = tag.getInt(TAG_NEAR);
            near = dist < 3.2D && !player.isCreative() ? near + 2 : Math.max(0, near - 1);
            if (near > 60) {
                reveal(v, player);
                continue;
            }
            tag.putInt(TAG_NEAR, near);
            if (dist < 10.0D && player.tickCount % 120 == 0) {
                level.playSound(null, v.blockPosition(), SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 0.5F, 0.4F);
            }
        }
    }

    private static void lookAt(Villager v, Player target) {
        double dx = target.getX() - v.getX();
        double dz = target.getZ() - v.getZ();
        double dy = target.getEyeY() - v.getEyeY();
        float yaw = (float) (Mth.atan2(dz, dx) * 57.29578D) - 90.0F;
        float pitch = (float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 57.29578D);
        v.setYRot(yaw);
        v.setXRot(pitch);
        v.setYHeadRot(yaw);
        v.setYBodyRot(yaw);
    }

    /** Маска упала: на месте жителя стоит настоящий Фокусник. */
    public static void reveal(Villager v, Player player) {
        if (!(v.level() instanceof ServerLevel level)) {
            return;
        }
        double x = v.getX();
        double y = v.getY();
        double z = v.getZ();
        float yaw = v.getYRot();
        v.discard();
        MagicUtil.poof(level, x, y + 1.0D, z);
        MagicianEntity m = ModEntities.MAGICIAN.get().create(level);
        if (m != null) {
            m.moveTo(x, y, z, yaw, 0.0F);
            m.setTarget(player);
            level.addFreshEntity(m);
        }
        level.playSound(null, BlockPos.containing(x, y, z), ModSounds.LAUGH.get(), SoundSource.HOSTILE, 2.0F, 0.8F);
        if (player instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.literal("§4§oЖитель улыбнулся. Слишком широко."), true);
            sp.addEffect(new MobEffectInstance(ModEffects.STAGE_FRIGHT, 200, 0));
            PerformanceDirector.addTension(sp, 25);
            Screamer.trigger(sp);
        }
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (isImpostor(event.getTarget())) {
            if (!event.getLevel().isClientSide && event.getTarget() instanceof Villager v) {
                reveal(v, event.getEntity());
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (isImpostor(event.getTarget())) {
            if (!event.getEntity().level().isClientSide && event.getTarget() instanceof Villager v) {
                reveal(v, event.getEntity());
            }
            event.setCanceled(true);
        }
    }
}
