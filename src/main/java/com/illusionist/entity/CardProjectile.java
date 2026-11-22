package com.illusionist.entity;

import com.illusionist.registry.ModEffects;
import com.illusionist.registry.ModEntities;
import com.illusionist.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Летящая игральная карта. Без гравитации, живёт 3 секунды. */
public class CardProjectile extends ThrowableItemProjectile {
    public CardProjectile(EntityType<? extends CardProjectile> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public CardProjectile(Level level, LivingEntity shooter) {
        super(ModEntities.CARD.get(), shooter, level);
        this.setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.MAGIC_CARD.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        } else if (this.tickCount > 60) {
            this.discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (entity instanceof MagicianEntity && this.getOwner() instanceof MagicianEntity) {
            return false;
        }
        return super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        Entity owner = this.getOwner();
        // Карты без владельца (дождь/номера Режиссёра) считаются картами Фокусника
        boolean fromMagician = owner == null || owner instanceof MagicianEntity;
        float damage = fromMagician ? 4.0F : 3.0F;
        if (target.hurt(this.level().damageSources().thrown(this, owner), damage)
                && fromMagician && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(ModEffects.STAGE_FRIGHT, 240, 0));
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, this.getX(), this.getY(), this.getZ(), 4, 0.1, 0.1, 0.1, 0.02);
            this.discard();
        }
    }
}
