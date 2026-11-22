package com.illusionist.entity;

import com.illusionist.registry.ModEntities;
import com.illusionist.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** Дымовая шашка: облако слепоты, мобы теряют цель, бросивший невидим 5 секунд. */
public class SmokeBombProjectile extends ThrowableItemProjectile {
    public SmokeBombProjectile(EntityType<? extends SmokeBombProjectile> type, Level level) {
        super(type, level);
    }

    public SmokeBombProjectile(Level level, LivingEntity shooter) {
        super(ModEntities.SMOKE_BOMB.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.SMOKE_BOMB.get();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        AreaEffectCloud cloud = new AreaEffectCloud(serverLevel, this.getX(), this.getY(), this.getZ());
        cloud.setRadius(3.5F);
        cloud.setDuration(200);
        cloud.setWaitTime(0);
        cloud.setRadiusPerTick(-3.5F / 200F);
        cloud.setParticle(ParticleTypes.LARGE_SMOKE);
        cloud.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
        if (this.getOwner() instanceof LivingEntity owner) {
            cloud.setOwner(owner);
            owner.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0, false, false));
        }
        serverLevel.addFreshEntity(cloud);

        for (Mob mob : serverLevel.getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(6.0))) {
            if (mob.getTarget() != null && mob.getTarget() == this.getOwner()) {
                mob.setTarget(null);
                mob.getNavigation().stop();
            }
        }
        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.NEUTRAL, 1.0F, 0.6F);
        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.3, this.getZ(),
                40, 1.2, 0.6, 1.2, 0.02);
        this.discard();
    }
}
