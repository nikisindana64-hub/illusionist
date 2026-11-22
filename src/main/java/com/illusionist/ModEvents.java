package com.illusionist;

import com.illusionist.entity.KindMagicianEntity;
import com.illusionist.entity.MagicianEntity;
import com.illusionist.registry.ModEntities;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

@EventBusSubscriber(modid = IllusionistMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModEvents {
    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.MAGICIAN.get(), MagicianEntity.createAttributes().build());
        event.put(ModEntities.KIND_MAGICIAN.get(), KindMagicianEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(ModEntities.MAGICIAN.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                MagicianEntity::checkSpawn,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
