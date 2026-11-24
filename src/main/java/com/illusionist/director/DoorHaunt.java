package com.illusionist.director;

import com.illusionist.entity.MagicianEntity;
import com.illusionist.registry.ModSounds;
import com.illusionist.util.DelayedTasks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

/**
 * «Медленная дверь» как в хоррорах: долгий скрип петель, тишина... и закрытая дверь неподалёку
 * сама отворяется. Иногда в проёме, чуть дальше, стоит неподвижная фигура.
 */
public final class DoorHaunt {
    private DoorHaunt() {
    }

    public static boolean slowOpen(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos c = player.blockPosition();
        List<BlockPos> doors = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-14, -3, -14), c.offset(14, 4, 14))) {
            BlockState st = level.getBlockState(p);
            if (st.is(BlockTags.WOODEN_DOORS)
                    && st.hasProperty(DoorBlock.OPEN) && !st.getValue(DoorBlock.OPEN)
                    && st.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                    && p.distSqr(c) > 20.0D) {
                doors.add(p.immutable());
            }
        }
        if (doors.isEmpty()) {
            return false;
        }
        BlockPos door = doors.get(level.random.nextInt(doors.size()));
        level.playSound(null, door, ModSounds.DOOR_CREAK.get(), SoundSource.BLOCKS, 1.6F, 0.9F);
        level.sendParticles(ParticleTypes.ASH, door.getX() + 0.5, door.getY() + 1.0, door.getZ() + 0.5, 6, 0.3, 0.4, 0.3, 0.0);

        // дверь поддаётся только в самом конце скрипа
        DelayedTasks.schedule(level, 76, () -> {
            BlockState now = level.getBlockState(door);
            if (!now.is(BlockTags.WOODEN_DOORS) || !now.hasProperty(DoorBlock.OPEN) || now.getValue(DoorBlock.OPEN)) {
                return;
            }
            level.setBlock(door, now.setValue(DoorBlock.OPEN, true), 10);
            player.displayClientMessage(Component.literal("§4§oДверь отворилась сама. Медленно."), true);
        });

        // с шансом 40% в проёме, за дверью, кто-то стоит
        if (level.random.nextFloat() < 0.4F) {
            DelayedTasks.schedule(level, 130, () -> {
                Vec3 away = new Vec3(door.getX() + 0.5 - player.getX(), 0.0, door.getZ() + 0.5 - player.getZ());
                if (away.lengthSqr() < 0.01D) {
                    return;
                }
                away = away.normalize().scale(2.5D);
                MagicianEntity.spawnIllusion(level, door.getX() + 0.5 + away.x, door.getY(), door.getZ() + 0.5 + away.z, null);
            });
        }
        return true;
    }
}
