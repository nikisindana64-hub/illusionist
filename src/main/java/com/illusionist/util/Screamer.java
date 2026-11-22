package com.illusionist.util;

import com.illusionist.Config;
import com.illusionist.registry.ModEffects;
import com.illusionist.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Скример: короткий эффект JUMPSCARE (клиент рисует лицо на весь экран) + крик только для этого игрока.
 * Не чаще, чем раз в 45 секунд на игрока, и отключается в конфиге.
 */
public final class Screamer {
    private static final String TAG = "illusionist_screamer_at";
    private static final long COOLDOWN = 900L;

    private Screamer() {
    }

    public static boolean trigger(ServerPlayer player) {
        return trigger(player, false);
    }

    public static boolean trigger(ServerPlayer player, boolean ignoreCooldown) {
        if (!Config.SCREAMERS.get() || player.isCreative() || player.isSpectator()) {
            return false;
        }
        long now = player.serverLevel().getGameTime();
        long last = player.getPersistentData().getLong(TAG);
        if (!ignoreCooldown && last != 0L && now - last < COOLDOWN) {
            return false;
        }
        player.getPersistentData().putLong(TAG, now);
        player.addEffect(new MobEffectInstance(ModEffects.JUMPSCARE, 18, 0, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 14, 0, false, false, false));
        player.playNotifySound(ModSounds.SCREAM.get(), SoundSource.HOSTILE, 1.8F, 0.95F + player.getRandom().nextFloat() * 0.1F);
        return true;
    }
}
