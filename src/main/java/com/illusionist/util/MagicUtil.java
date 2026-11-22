package com.illusionist.util;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class MagicUtil {
    private static final SoundEvent[] FAKE_SOUNDS = {
            SoundEvents.ENDERMAN_STARE,
            SoundEvents.CREEPER_PRIMED,
            SoundEvents.ZOMBIE_AMBIENT,
            SoundEvents.SKELETON_AMBIENT,
            SoundEvents.WOODEN_DOOR_OPEN,
            SoundEvents.CHEST_OPEN,
            SoundEvents.ILLUSIONER_PREPARE_MIRROR,
            SoundEvents.GHAST_AMBIENT,
            SoundEvents.WITCH_AMBIENT,
            SoundEvents.CAT_HISS,
            SoundEvents.SPIDER_AMBIENT,
            SoundEvents.ENDERMAN_AMBIENT
    };

    /** Облачко дыма «пуф». */
    public static void poof(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.POOF, x, y, z, 18, 0.35, 0.6, 0.35, 0.04);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 10, 0.3, 0.6, 0.3, 0.02);
    }

    /** Ложный звук где-то рядом, чаще — за спиной жертвы. */
    public static void fakeSound(ServerLevel level, Entity victim) {
        double angle = level.random.nextDouble() * Math.PI * 2;
        double dist = 3 + level.random.nextDouble() * 6;
        double x = victim.getX() + Math.cos(angle) * dist;
        double z = victim.getZ() + Math.sin(angle) * dist;
        SoundEvent sound = FAKE_SOUNDS[level.random.nextInt(FAKE_SOUNDS.length)];
        level.playSound(null, x, victim.getY(), z, sound, SoundSource.HOSTILE,
                0.8F, 0.7F + level.random.nextFloat() * 0.5F);
    }

    /** Ищет место, где можно стоять: пустые две клетки, твёрдый пол, нет жидкости. */
    @Nullable
    public static BlockPos findStandable(Level level, double x, double startY, double z) {
        int bx = Mth.floor(x);
        int bz = Mth.floor(z);
        int top = Mth.floor(startY) + 3;
        int bottom = Mth.floor(startY) - 6;
        for (int y = top; y >= bottom; y--) {
            BlockPos feet = new BlockPos(bx, y, bz);
            if (isFree(level, feet) && isFree(level, feet.above()) && isSolidFloor(level, feet.below())) {
                return feet;
            }
        }
        return null;
    }

    private static boolean isFree(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getCollisionShape(level, pos).isEmpty() && state.getFluidState().isEmpty();
    }

    private static boolean isSolidFloor(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.getCollisionShape(level, pos).isEmpty() && state.getFluidState().isEmpty();
    }

    /** Гасит факелы, свечи и костры вокруг точки (учитывает mobGriefing). Возвращает true, если что-то погасло. */
    public static boolean snuffLights(ServerLevel level, BlockPos center, int radius) {
        if (!level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING)) {
            return false;
        }
        boolean any = false;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -4, -radius), center.offset(radius, 4, radius))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(net.minecraft.world.level.block.Blocks.TORCH) || state.is(net.minecraft.world.level.block.Blocks.WALL_TORCH)
                    || state.is(net.minecraft.world.level.block.Blocks.SOUL_TORCH)
                    || state.is(net.minecraft.world.level.block.Blocks.SOUL_WALL_TORCH)) {
                level.destroyBlock(pos, true);
                any = true;
            } else if ((state.getBlock() instanceof net.minecraft.world.level.block.CampfireBlock
                    || state.getBlock() instanceof net.minecraft.world.level.block.CandleBlock)
                    && state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)
                    && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)) {
                level.setBlock(pos, state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT, false), 3);
                any = true;
            }
        }
        if (any) {
            level.playSound(null, center, SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1.0F, 0.6F);
        }
        return any;
    }

    /** Кролик-убийца «из шляпы». */
    @Nullable
    public static Rabbit spawnHatRabbit(ServerLevel level, double x, double y, double z, @Nullable LivingEntity target) {
        Rabbit rabbit = EntityType.RABBIT.create(level);
        if (rabbit == null) {
            return null;
        }
        rabbit.moveTo(x, y, z, level.random.nextFloat() * 360F, 0F);
        rabbit.setVariant(Rabbit.Variant.EVIL);
        rabbit.setCustomName(Component.translatable("entity.illusionist.hat_rabbit"));
        if (target != null) {
            rabbit.setTarget(target);
        }
        level.addFreshEntity(rabbit);
        poof(level, x, y + 0.3, z);
        return rabbit;
    }
}
