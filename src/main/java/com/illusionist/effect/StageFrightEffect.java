package com.illusionist.effect;

import com.illusionist.IllusionistMod;
import com.illusionist.util.MagicUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** «Сценический страх»: замедляет и подсовывает ложные звуки за спиной. */
public class StageFrightEffect extends MobEffect {
    public StageFrightEffect() {
        super(MobEffectCategory.HARMFUL, 0x7A0B1A);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED,
                IllusionistMod.id("effect.stage_fright"),
                -0.12D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 40 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level() instanceof ServerLevel level && entity.getRandom().nextInt(3) == 0) {
            MagicUtil.fakeSound(level, entity);
        }
        return true;
    }
}
