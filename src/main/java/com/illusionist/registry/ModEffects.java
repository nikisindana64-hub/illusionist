package com.illusionist.registry;

import com.illusionist.IllusionistMod;
import com.illusionist.effect.HypnosisEffect;
import com.illusionist.effect.JumpscareEffect;
import com.illusionist.effect.StageFrightEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, IllusionistMod.MODID);

    public static final DeferredHolder<MobEffect, StageFrightEffect> STAGE_FRIGHT =
            EFFECTS.register("stage_fright", StageFrightEffect::new);

    public static final DeferredHolder<MobEffect, JumpscareEffect> JUMPSCARE =
            EFFECTS.register("jumpscare", JumpscareEffect::new);

    public static final DeferredHolder<MobEffect, HypnosisEffect> HYPNOSIS =
            EFFECTS.register("hypnosis", HypnosisEffect::new);
}
