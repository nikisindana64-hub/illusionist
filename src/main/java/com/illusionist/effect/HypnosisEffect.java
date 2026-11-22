package com.illusionist.effect;

import com.illusionist.entity.MagicianEntity;
import com.illusionist.item.MonocleOfTruthItem;
import java.util.List;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** «Гипноз»: мягко тянет к ближайшему настоящему Фокуснику. Монокль Истины защищает. */
public class HypnosisEffect extends MobEffect {
    public HypnosisEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B0E2E);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 4 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof Player player && MonocleOfTruthItem.isProtected(player)) {
            return false; // эффект снимается
        }
        List<MagicianEntity> list = entity.level().getEntitiesOfClass(MagicianEntity.class,
                entity.getBoundingBox().inflate(24.0D), m -> !m.isIllusion());
        if (!list.isEmpty()) {
            Vec3 pull = list.get(0).position().subtract(entity.position()).multiply(1, 0, 1).normalize().scale(0.11D);
            entity.setDeltaMovement(entity.getDeltaMovement().add(pull.x, 0.0, pull.z));
            entity.hurtMarked = true;
        }
        return true;
    }
}
