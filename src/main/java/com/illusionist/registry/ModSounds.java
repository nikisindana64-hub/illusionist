package com.illusionist.registry;

import com.illusionist.IllusionistMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Свои звуки (assets/illusionist/sounds/*.ogg, описаны в sounds.json). */
public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, IllusionistMod.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_BOX_NOTE = register("music_box_note");
    public static final DeferredHolder<SoundEvent, SoundEvent> APPLAUSE = register("applause");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCREAM = register("scream");
    public static final DeferredHolder<SoundEvent, SoundEvent> LAUGH = register("laugh");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRUMROLL = register("drumroll");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(IllusionistMod.id(name)));
    }
}
