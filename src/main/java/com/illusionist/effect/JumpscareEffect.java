package com.illusionist.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Служебный эффект «Скример»: клиент рисует во весь экран лицо, пока эффект активен. Без логики. */
public class JumpscareEffect extends MobEffect {
    public JumpscareEffect() {
        super(MobEffectCategory.HARMFUL, 0xFFFFFF);
    }
}
