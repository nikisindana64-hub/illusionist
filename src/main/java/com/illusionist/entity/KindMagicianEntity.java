package com.illusionist.entity;

import com.illusionist.director.PerformanceDirector;
import com.illusionist.registry.ModEffects;
import com.illusionist.registry.ModEntities;
import com.illusionist.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Добрый Фокусник («шоумен»): днём приветливо показывает пять добрых фокусов по ПКМ.
 * Ловушка: если его ударить — или (20%) глубокой ночью в акте III+ — маска трескается,
 * и перед тобой настоящий Фокусник. Идея: Gemini-версия мода.
 */
public class KindMagicianEntity extends PathfinderMob implements MagicianPose {
    private static final EntityDataAccessor<Integer> DATA_CAST =
            SynchedEntityData.defineId(KindMagicianEntity.class, EntityDataSerializers.INT);

    private int trickCooldown = 0;
    private int lifeTicks = 2400; // гостит ~2 минуты, затем исчезает в искрах
    private int tricksShown = 0;

    public KindMagicianEntity(EntityType<? extends KindMagicianEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CAST, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(4, new RandomStrollGoal(this, 0.75D));
    }

    @Override
    public boolean isWatchedPose() {
        return false;
    }

    @Override
    public int getCastTicks() {
        return this.entityData.get(DATA_CAST);
    }

    @Override
    public int getSkinVariant() {
        return 2;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel level) {
            int cast = getCastTicks();
            if (cast > 0) {
                this.entityData.set(DATA_CAST, cast - 1);
            }
            if (trickCooldown > 0) {
                trickCooldown--;
            }
            if (--lifeTicks <= 0) {
                level.sendParticles(ParticleTypes.FIREWORK, getX(), getY() + 1.4, getZ(), 35, 0.5, 1.0, 0.5, 0.08);
                level.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0F, 1.3F);
                this.discard();
            }
        } else if (this.tickCount % 6 == 0) {
            this.level().addParticle(ParticleTypes.END_ROD, getRandomX(0.5), getY() + 0.6 + random.nextDouble() * 2.0,
                    getRandomZ(0.5), 0.0, 0.02, 0.0);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!(this.level() instanceof ServerLevel level) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (trickCooldown > 0) {
            sp.displayClientMessage(Component.literal("§6[Добрый Фокусник]§f Секундочку, готовлю следующий номер!"), true);
            return InteractionResult.SUCCESS;
        }
        // Глубокой ночью в акте III+ добрый облик может оказаться ловушкой
        if (PerformanceDirector.getAct(sp) >= 3 && level.isNight() && tricksShown >= 1 && random.nextFloat() < 0.22F) {
            reveal(sp, level, "§4[Фокусник]§c Ха-ха-ха! Ты правда поверил в добрые фокусы в такую ночь?!");
            return InteractionResult.SUCCESS;
        }
        performTrick(sp, level);
        trickCooldown = 80;
        tricksShown++;
        return InteractionResult.SUCCESS;
    }

    private void performTrick(ServerPlayer player, ServerLevel level) {
        this.entityData.set(DATA_CAST, 24);
        level.playSound(null, blockPosition(), ModSounds.APPLAUSE.get(), SoundSource.NEUTRAL, 0.6F, 1.1F);
        RandomSource rng = this.random;
        switch (rng.nextInt(5)) {
            case 0 -> { // «Монетка за ухом»
                ItemStack[] gifts = {
                        new ItemStack(Items.EMERALD, 1 + rng.nextInt(2)),
                        new ItemStack(Items.GOLD_INGOT, 1 + rng.nextInt(3)),
                        new ItemStack(Items.GOLDEN_APPLE, 1),
                        new ItemStack(Items.CAKE, 1),
                        new ItemStack(Items.FIREWORK_ROCKET, 4)
                };
                ItemStack gift = gifts[rng.nextInt(gifts.length)];
                if (!player.getInventory().add(gift.copy())) {
                    this.spawnAtLocation(gift.copy());
                }
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.2, player.getZ(), 18, 0.5, 0.5, 0.5, 0.05);
                level.playSound(null, blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 1.0F, 1.3F);
                player.displayClientMessage(Component.literal("§6[Добрый Фокусник]§f Ловкость рук! Смотри, что нашлось за ухом: §e"
                        + gift.getHoverName().getString()), true);
            }
            case 1 -> { // «Кролик-талисман»
                Rabbit bunny = EntityType.RABBIT.create(level);
                if (bunny != null) {
                    bunny.setVariant(Rabbit.Variant.WHITE);
                    bunny.moveTo(getX(), getY() + 2.2, getZ(), rng.nextFloat() * 360F, 0);
                    level.addFreshEntity(bunny);
                }
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 1800, 1));
                level.sendParticles(ParticleTypes.FIREWORK, getX(), getY() + 2.3, getZ(), 25, 0.4, 0.4, 0.4, 0.08);
                level.playSound(null, blockPosition(), SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.NEUTRAL, 1.0F, 1.2F);
                player.displayClientMessage(Component.literal("§6[Добрый Фокусник]§f Та-дам! Белый кролик из цилиндра приносит Удачу!"), true);
            }
            case 2 -> { // «Цветочный каскад»
                Block[] flowers = {Blocks.POPPY, Blocks.DANDELION, Blocks.BLUE_ORCHID, Blocks.LILY_OF_THE_VALLEY};
                BlockPos center = player.blockPosition();
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        BlockPos p = center.offset(dx, 0, dz);
                        if (level.getBlockState(p).isAir() && level.getBlockState(p.below()).is(Blocks.GRASS_BLOCK)
                                && rng.nextFloat() < 0.45F) {
                            level.setBlock(p, flowers[rng.nextInt(flowers.length)].defaultBlockState(), 3);
                        }
                    }
                }
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1));
                player.getFoodData().eat(4, 0.5F);
                level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.4, player.getZ(), 10, 0.6, 0.5, 0.6, 0.02);
                level.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0F, 1.25F);
                player.displayClientMessage(Component.literal("§6[Добрый Фокусник]§f «Цветочный каскад»! Немного магии для настроения."), true);
            }
            case 3 -> { // «Звёздный салют»: снимает страхи
                player.removeEffect(ModEffects.STAGE_FRIGHT);
                player.removeEffect(ModEffects.HYPNOSIS);
                player.removeEffect(MobEffects.DARKNESS);
                player.removeEffect(MobEffects.BLINDNESS);
                PerformanceDirector.addTension(player, -15);
                level.sendParticles(ParticleTypes.FIREWORK, getX(), getY() + 2.0, getZ(), 45, 0.9, 0.9, 0.9, 0.14);
                level.playSound(null, blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.NEUTRAL, 1.0F, 1.3F);
                player.displayClientMessage(Component.literal("§6[Добрый Фокусник]§f Звёздный салют! Все страхи и иллюзии развеяны!"), true);
            }
            default -> { // «Секрет из рукава»
                int roll = rng.nextInt(4);
                String buff;
                if (roll == 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2400, 1));
                    buff = "Скорость II";
                } else if (roll == 1) {
                    player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 2400, 1));
                    buff = "Спешка II";
                } else if (roll == 2) {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 2400, 0));
                    buff = "Ночное зрение";
                } else {
                    player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 1800, 0));
                    buff = "Плавное падение";
                }
                level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.2, player.getZ(), 24, 0.5, 0.8, 0.5, 0.06);
                level.playSound(null, blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.NEUTRAL, 1.0F, 1.4F);
                player.displayClientMessage(Component.literal("§6[Добрый Фокусник]§f Секрет из рукава — держи §b" + buff + "§f!"), true);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level() instanceof ServerLevel level && source.getEntity() instanceof ServerPlayer attacker) {
            reveal(attacker, level, "§4[Фокусник]§c Ты ударил артиста?! Что ж... доброе представление ОКОНЧЕНО!");
            return false;
        }
        return super.hurt(source, amount);
    }

    private void reveal(ServerPlayer target, ServerLevel level, String message) {
        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.4, getZ(), 50, 0.6, 1.2, 0.6, 0.07);
        level.sendParticles(ParticleTypes.CRIMSON_SPORE, getX(), getY() + 1.5, getZ(), 45, 0.7, 1.2, 0.7, 0.1);
        level.playSound(null, blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.1F, 0.65F);
        target.displayClientMessage(Component.literal(message), false);

        MagicianEntity nightmare = ModEntities.MAGICIAN.get().create(level);
        if (nightmare != null) {
            nightmare.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
            nightmare.setTarget(target);
            nightmare.setPersistenceRequired();
            level.addFreshEntity(nightmare);
        }
        // Предательство сразу двигает представление вперёд
        PerformanceDirector.addTension(target, 30);
        if (PerformanceDirector.getAct(target) < 2) {
            PerformanceDirector.setAct(target, 2);
            PerformanceDirector.announceAct(target, 2);
        }
        this.discard();
    }
}
