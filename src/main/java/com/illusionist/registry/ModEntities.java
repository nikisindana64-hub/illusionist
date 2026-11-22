package com.illusionist.registry;

import com.illusionist.IllusionistMod;
import com.illusionist.entity.CardProjectile;
import com.illusionist.entity.KindMagicianEntity;
import com.illusionist.entity.MagicianEntity;
import com.illusionist.entity.SmokeBombProjectile;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, IllusionistMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<MagicianEntity>> MAGICIAN =
            ENTITIES.register("magician", () -> EntityType.Builder
                    .of(MagicianEntity::new, MobCategory.MONSTER)
                    .sized(0.65F, 2.75F)
                    .eyeHeight(2.35F)
                    .clientTrackingRange(10)
                    .build("magician"));

    public static final DeferredHolder<EntityType<?>, EntityType<KindMagicianEntity>> KIND_MAGICIAN =
            ENTITIES.register("kind_magician", () -> EntityType.Builder
                    .of(KindMagicianEntity::new, MobCategory.CREATURE)
                    .sized(0.65F, 2.75F)
                    .eyeHeight(2.35F)
                    .clientTrackingRange(10)
                    .build("kind_magician"));

    public static final DeferredHolder<EntityType<?>, EntityType<CardProjectile>> CARD =
            ENTITIES.register("card", () -> EntityType.Builder
                    .<CardProjectile>of(CardProjectile::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("card"));

    public static final DeferredHolder<EntityType<?>, EntityType<SmokeBombProjectile>> SMOKE_BOMB =
            ENTITIES.register("smoke_bomb", () -> EntityType.Builder
                    .<SmokeBombProjectile>of(SmokeBombProjectile::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("smoke_bomb"));
}
