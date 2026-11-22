package com.illusionist.entity;

import com.illusionist.Config;
import com.illusionist.director.PerformanceDirector;
import com.illusionist.registry.ModEffects;
import com.illusionist.registry.ModEntities;
import com.illusionist.registry.ModItems;
import com.illusionist.registry.ModSounds;
import com.illusionist.item.MonocleOfTruthItem;
import com.illusionist.util.MagicUtil;
import com.illusionist.util.Screamer;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Фокусник. Правила:
 *  - Пока на него смотрят — он замирает, музыка стихает. Отвернулся — он идёт и играет мелодию.
 *  - Если смотреть слишком долго, он «моргает» и оказывается у тебя за спиной.
 *  - Бросает карты, гасит свет, подделывает звуки, создаёт иллюзии (одна из них — настоящий он),
 *    крадёт вещи (вернутся после его смерти), достаёт из шляпы кроликов-убийц.
 *  - Ниже 30% здоровья начинается «Последний акт».
 */
public class MagicianEntity extends Monster implements MagicianPose {
    private static final EntityDataAccessor<Integer> DATA_CAST =
            SynchedEntityData.defineId(MagicianEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_ENRAGED =
            SynchedEntityData.defineId(MagicianEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> DATA_WATCHED =
            SynchedEntityData.defineId(MagicianEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_ILLUSION =
            SynchedEntityData.defineId(MagicianEntity.class, EntityDataSerializers.BOOLEAN);

    private static final float[] MELODY = {
            1.00F, 1.19F, 1.50F, 1.19F, 1.00F, 0.89F, 0.84F, 0.89F,
            1.00F, 1.19F, 1.34F, 1.19F, 1.00F, 0.84F, 0.75F, 0.84F
    };

    private final List<ItemStack> stolen = new ArrayList<>();
    private int watchedTicks;
    private int cardCd = 60;
    private int vanishCd = 200;
    private int cloneCd = 300;
    private int stealCd = 400;
    private int lightsCd = 300;
    private int soundCd = 100;
    private int laughCd = 500;
    private int rabbitCd = 500;
    private int finaleCd = 100;
    private int dodgeCd = 0;
    private int beatCd = 0;
    private int noteIdx = 0;
    private int illusionAge = 0;

    public MagicianEntity(EntityType<? extends MagicianEntity> type, Level level) {
        super(type, level);
        this.xpReward = 30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    public static boolean checkSpawn(EntityType<MagicianEntity> type, ServerLevelAccessor level,
                                     MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (reason == MobSpawnType.SPAWNER || reason == MobSpawnType.SPAWN_EGG || reason == MobSpawnType.COMMAND) {
            return true;
        }
        if (!Config.NATURAL_SPAWN.get()) {
            return false;
        }
        return Monster.checkMonsterSpawnRules(type, level, reason, pos, random)
                && random.nextInt(Config.SPAWN_CHANCE.get()) == 0;
    }

    // ------------------------------------------------------------------ данные

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_WATCHED, false);
        builder.define(DATA_ILLUSION, false);
        builder.define(DATA_CAST, 0);
        builder.define(DATA_ENRAGED, false);
    }

    public boolean isWatched() {
        return this.entityData.get(DATA_WATCHED);
    }

    @Override
    public boolean isWatchedPose() {
        return isWatched();
    }

    @Override
    public int getCastTicks() {
        return this.entityData.get(DATA_CAST);
    }

    private void cast(int ticks) {
        this.entityData.set(DATA_CAST, ticks);
    }

    public boolean isEnraged() {
        return this.entityData.get(DATA_ENRAGED);
    }

    @Override
    public int getSkinVariant() {
        return isEnraged() ? 1 : 0;
    }

    private void setWatched(boolean value) {
        this.entityData.set(DATA_WATCHED, value);
    }

    public boolean isIllusion() {
        return this.entityData.get(DATA_ILLUSION);
    }

    public void setIllusion(boolean value) {
        this.entityData.set(DATA_ILLUSION, value);
        if (value) {
            this.setSilent(true);
            this.xpReward = 0;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Illusion", isIllusion());
        tag.putBoolean("Enraged", isEnraged());
        tag.putInt("IllusionAge", illusionAge);
        ListTag list = new ListTag();
        for (ItemStack stack : stolen) {
            list.add(stack.save(this.level().registryAccess()));
        }
        tag.put("Stolen", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setIllusion(tag.getBoolean("Illusion"));
        this.entityData.set(DATA_ENRAGED, tag.getBoolean("Enraged"));
        illusionAge = tag.getInt("IllusionAge");
        stolen.clear();
        ListTag list = tag.getList("Stolen", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            ItemStack.parse(this.level().registryAccess(), list.getCompound(i)).ifPresent(stolen::add);
        }
        if (!stolen.isEmpty()) {
            this.setPersistenceRequired();
        }
    }

    // ------------------------------------------------------------------ ИИ

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // В Акте I Фокусник только наблюдает. Нападает с Акта II (или если его ударили).
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 0, false, false,
                entity -> entity instanceof Player player && !wearsHat(player)
                        && (isIllusion() || actOf(player) >= 2)));
    }

    public static int actOf(Player player) {
        return player instanceof ServerPlayer sp ? PerformanceDirector.getAct(sp) : 1;
    }

    public static boolean wearsHat(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.TOP_HAT.get());
    }

    /** Смотрит ли игрок на Фокусника (конус ~20° и прямая видимость). */
    private boolean isWatchedBy(Player player) {
        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 toMe = new Vec3(this.getX() - player.getX(), this.getEyeY() - player.getEyeY(), this.getZ() - player.getZ());
        double dist = toMe.length();
        if (dist < 0.001D) {
            return true;
        }
        return look.dot(toMe.scale(1.0D / dist)) > 0.94D && player.hasLineOfSight(this);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (isIllusion()) {
            if (++illusionAge > 300) {
                vanishIllusion();
                return;
            }
        }
        tickCooldowns();

        Player player = level.getNearestPlayer(this, 48.0D);
        if (player == null || player.isCreative() || player.isSpectator()) {
            setWatched(false);
            return;
        }
        double dist = this.distanceTo(player);
        boolean peaceful = wearsHat(player) && this.getTarget() == null;
        boolean real = !isIllusion();

        int act = actOf(player);

        // Психологическое давление работает независимо от взгляда
        if (real && !peaceful) {
            if (soundCd <= 0 && dist < 32) {
                MagicUtil.fakeSound(level, player);
                soundCd = 120 + this.random.nextInt(260);
            }
            if (act >= 2 && laughCd <= 0 && dist < 22) {
                level.playSound(null, this.getX(), this.getY() + 1.5, this.getZ(), ModSounds.LAUGH.get(),
                        SoundSource.HOSTILE, 1.6F, 0.8F + this.random.nextFloat() * 0.3F);
                laughCd = 700 + this.random.nextInt(900);
            }
            if (act >= 2 && lightsCd <= 0 && dist < 24 && Config.LIGHTS_OUT.get()) {
                snuffLights(level);
                lightsCd = 700;
            }
        }

        // Правило «не моргай»
        boolean watched = isWatchedBy(player);
        setWatched(watched);
        if (watched) {
            this.getNavigation().stop();
            this.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 0.0D);
            this.getLookControl().setLookAt(player, 30.0F, 30.0F);
            watchedTicks++;
            if (!real && this.getTarget() == null && watchedTicks > 50) {
                // «Наблюдатель» — далёкая иллюзия, исчезает, стоит на неё посмотреть
                vanishIllusion();
                return;
            }
            if (real && !peaceful && watchedTicks > 160) {
                watchedTicks = 0;
                // Не выдержал взгляд — либо «моргнёт» за спину, либо загипнотизирует
                if (act >= 2 && this.random.nextBoolean() && !MonocleOfTruthItem.isProtected(player)) {
                    player.addEffect(new MobEffectInstance(ModEffects.HYPNOSIS, 140, 0));
                    level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_PREPARE_BLINDNESS,
                            SoundSource.HOSTILE, 1.5F, 0.5F);
                    cast(30);
                } else {
                    vanishBehind(level, player);
                }
            }
            return;
        }
        watchedTicks = Math.max(0, watchedTicks - 2);
        if (!real || peaceful) {
            return;
        }

        playMusicAndHeartbeat(level, player, dist);

        float hp = this.getHealth() / this.getMaxHealth();
        boolean finale = hp < 0.3F;
        if (finale && !isEnraged()) {
            startFinale(level);
        }

        if (act >= 2 && cardCd <= 0 && dist >= 4 && dist <= 26 && this.hasLineOfSight(player)) {
            throwCards(level, player, finale ? 5 : (hp < 0.6F || act >= 3 ? 3 : 1));
            cardCd = (finale ? 40 : 80) + this.random.nextInt(40);
        } else if (vanishCd <= 0 && dist >= 10) {
            vanishBehind(level, player);
            vanishCd = 260 + this.random.nextInt(200);
        }
        if (act >= 2 && cloneCd <= 0 && (hp < 0.8F || act >= 3) && dist < 30) {
            spawnIllusions(level, player, finale ? 4 : (act >= 3 ? 3 : 2));
            cloneCd = finale ? 360 : 560;
        }
        if (act >= 2 && rabbitCd <= 0 && (hp < 0.65F || act >= 3) && dist < 18) {
            for (int i = 0; i < (finale ? 3 : 1); i++) {
                MagicUtil.spawnHatRabbit(level, this.getX() + this.random.nextGaussian(), this.getY() + 2.2,
                        this.getZ() + this.random.nextGaussian(), player);
            }
            rabbitCd = 600;
        }
        if (finale && finaleCd <= 0) {
            lastAct(level);
            finaleCd = 500;
        }
    }

    private void tickCooldowns() {
        if (cardCd > 0) cardCd--;
        if (vanishCd > 0) vanishCd--;
        if (cloneCd > 0) cloneCd--;
        if (stealCd > 0) stealCd--;
        if (lightsCd > 0) lightsCd--;
        if (soundCd > 0) soundCd--;
        if (laughCd > 0) laughCd--;
        if (rabbitCd > 0) rabbitCd--;
        if (finaleCd > 0) finaleCd--;
        if (dodgeCd > 0) dodgeCd--;
        if (beatCd > 0) beatCd--;
        if (getCastTicks() > 0) cast(getCastTicks() - 1);
    }

    private void playMusicAndHeartbeat(ServerLevel level, Player player, double dist) {
        if (dist < 28 && this.tickCount % 8 == 0) {
            float pitch = MELODY[noteIdx++ % MELODY.length];
            level.playSound(null, this.getX(), this.getY() + 1.0, this.getZ(),
                    ModSounds.MUSIC_BOX_NOTE.get(), SoundSource.HOSTILE, 2.0F, pitch);
        }
        if (dist < 12 && beatCd <= 0) {
            level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.5F, 1.0F);
            beatCd = (int) (12 + dist * 2.5D);
        }
    }

    // ------------------------------------------------------------------ трюки

    private void throwCards(ServerLevel level, Player target, int count) {
        Vec3 from = this.getEyePosition();
        Vec3 aim = target.getEyePosition().subtract(0, 0.4, 0).subtract(from).normalize();
        for (int i = 0; i < count; i++) {
            float angle = (i - (count - 1) / 2.0F) * 0.18F;
            Vec3 dir = aim.yRot(angle);
            CardProjectile card = new CardProjectile(level, this);
            card.shoot(dir.x, dir.y, dir.z, 1.5F, 0.5F);
            level.addFreshEntity(card);
        }
        level.playSound(null, this.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.HOSTILE, 1.0F, 1.3F);
        cast(14);
    }

    /** Телепорт за спину игрока. */
    private void vanishBehind(ServerLevel level, Player player) {
        Vec3 back = player.getLookAngle().multiply(1, 0, 1);
        if (back.lengthSqr() < 0.01D) {
            back = new Vec3(1, 0, 0);
        }
        back = back.normalize().scale(-(3.5D + this.random.nextDouble() * 2.0D));
        BlockPos ground = MagicUtil.findStandable(level, player.getX() + back.x, player.getY(), player.getZ() + back.z);
        if (ground == null) {
            return;
        }
        MagicUtil.poof(level, this.getX(), this.getY() + 1.0, this.getZ());
        this.moveTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, this.getYRot(), this.getXRot());
        this.getNavigation().stop();
        MagicUtil.poof(level, this.getX(), this.getY() + 1.0, this.getZ());
        level.playSound(null, this.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 1.2F, 0.8F);
        if (!isIllusion() && player instanceof ServerPlayer sp && actOf(sp) >= 2 && this.random.nextInt(4) == 0
                && !MonocleOfTruthItem.isProtected(sp)) {
            Screamer.trigger(sp);
        }
    }

    /** «Где шарик?»: создаёт иллюзии и меняется местами с одной из них. */
    private void spawnIllusions(ServerLevel level, Player player, int count) {
        MagicianEntity swapWith = null;
        for (int i = 0; i < count; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2;
            double radius = 3 + this.random.nextDouble() * 4;
            MagicianEntity illusion = spawnIllusion(level,
                    this.getX() + Math.cos(angle) * radius, this.getY(), this.getZ() + Math.sin(angle) * radius, player);
            if (illusion != null && swapWith == null) {
                swapWith = illusion;
            }
        }
        if (swapWith != null) {
            Vec3 mine = this.position();
            Vec3 theirs = swapWith.position();
            this.moveTo(theirs.x, theirs.y, theirs.z, this.getYRot(), this.getXRot());
            swapWith.moveTo(mine.x, mine.y, mine.z, swapWith.getYRot(), swapWith.getXRot());
            this.getNavigation().stop();
        }
        level.playSound(null, this.blockPosition(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.HOSTILE, 1.2F, 1.0F);
        cast(24);
    }

    @Nullable
    public static MagicianEntity spawnIllusion(ServerLevel level, double x, double y, double z, @Nullable LivingEntity target) {
        BlockPos ground = MagicUtil.findStandable(level, x, y, z);
        if (ground == null) {
            return null;
        }
        MagicianEntity illusion = ModEntities.MAGICIAN.get().create(level);
        if (illusion == null) {
            return null;
        }
        illusion.setIllusion(true);
        illusion.moveTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, level.random.nextFloat() * 360F, 0F);
        if (target != null) {
            illusion.setTarget(target);
        }
        level.addFreshEntity(illusion);
        MagicUtil.poof(level, illusion.getX(), illusion.getY() + 1.0, illusion.getZ());
        return illusion;
    }

    /** Призыв настоящего Фокусника рядом с игроком. */
    @Nullable
    public static MagicianEntity summonNear(ServerLevel level, Player player) {
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double dist = 14 + level.random.nextDouble() * 5;
            BlockPos ground = MagicUtil.findStandable(level, player.getX() + Math.cos(angle) * dist,
                    player.getY(), player.getZ() + Math.sin(angle) * dist);
            if (ground == null) {
                continue;
            }
            MagicianEntity magician = ModEntities.MAGICIAN.get().create(level);
            if (magician == null) {
                return null;
            }
            magician.moveTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, 0F, 0F);
            magician.setTarget(player);
            magician.setPersistenceRequired();
            level.addFreshEntity(magician);
            MagicUtil.poof(level, magician.getX(), magician.getY() + 1.0, magician.getZ());
            level.playSound(null, magician.blockPosition(), SoundEvents.ILLUSIONER_PREPARE_BLINDNESS,
                    SoundSource.HOSTILE, 2.0F, 0.6F);
            return magician;
        }
        return null;
    }

    /** Гасит факелы, свечи и костры вокруг. */
    private void snuffLights(ServerLevel level) {
        MagicUtil.snuffLights(level, this.blockPosition(), 8);
    }

    private void startFinale(ServerLevel level) {
        this.entityData.set(DATA_ENRAGED, true);
        var speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.addOrUpdateTransientModifier(new AttributeModifier(
                    com.illusionist.IllusionistMod.id("finale_speed"), 0.35D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(40))) {
            p.displayClientMessage(Component.translatable("message.illusionist.finale"), true);
        }
        level.playSound(null, this.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 2.0F, 0.5F);
    }

    /** «Последний акт»: тьма, иллюзии, ужас. */
    private void lastAct(ServerLevel level) {
        for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(24))) {
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0));
            p.addEffect(new MobEffectInstance(ModEffects.STAGE_FRIGHT, 200, 0));
            MagicUtil.fakeSound(level, p);
        }
        level.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY() + 1.0, this.getZ(), 30, 0.8, 1.0, 0.8, 0.05);
    }

    // ------------------------------------------------------------------ бой

    @Override
    public boolean doHurtTarget(Entity target) {
        if (isWatched()) {
            return false;
        }
        if (isIllusion()) {
            if (target instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(ModEffects.STAGE_FRIGHT, 200, 0));
            }
            vanishIllusion();
            return true;
        }
        if (target instanceof ServerPlayer player && stealCd <= 0 && Config.ALLOW_STEALING.get()
                && !wearsHat(player) && trySteal(player)) {
            stealCd = 1200;
            if (this.level() instanceof ServerLevel level) {
                vanishBehind(level, player);
            }
            return true;
        }
        return super.doHurtTarget(target);
    }

    private boolean trySteal(ServerPlayer player) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            if (!player.getInventory().getItem(i).isEmpty()) {
                slots.add(i);
            }
        }
        if (slots.isEmpty()) {
            return false;
        }
        int slot = slots.get(this.random.nextInt(slots.size()));
        ItemStack taken = player.getInventory().getItem(slot).copy();
        ItemStack card = new ItemStack(ModItems.JOKER_CARD.get());
        card.set(DataComponents.CUSTOM_NAME, Component.translatable("message.illusionist.calling_card", taken.getHoverName()));
        player.getInventory().setItem(slot, card);
        stolen.add(taken);
        this.setPersistenceRequired();
        player.displayClientMessage(Component.translatable("message.illusionist.stolen", taken.getHoverName()), true);
        this.level().playSound(null, player.blockPosition(), ModSounds.LAUGH.get(), SoundSource.HOSTILE, 1.4F, 0.9F);
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isIllusion()) {
            if (!this.level().isClientSide) {
                if (source.getEntity() instanceof LivingEntity attacker) {
                    attacker.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                }
                vanishIllusion();
            }
            return true;
        }
        boolean result = super.hurt(source, amount);
        if (result && !this.level().isClientSide && dodgeCd <= 0
                && source.getEntity() instanceof Player attacker && this.random.nextInt(4) == 0
                && this.level() instanceof ServerLevel level) {
            vanishBehind(level, attacker);
            dodgeCd = 100;
        }
        return result;
    }

    /** Иллюзия растворяется в дыму. */
    public void vanishIllusion() {
        if (this.level() instanceof ServerLevel level && !this.isRemoved()) {
            MagicUtil.poof(level, this.getX(), this.getY() + 1.0, this.getZ());
            level.playSound(null, this.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 0.8F, 1.6F);
        }
        this.discard();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel level && !isIllusion()) {
            for (MagicianEntity other : level.getEntitiesOfClass(MagicianEntity.class, this.getBoundingBox().inflate(64),
                    MagicianEntity::isIllusion)) {
                other.vanishIllusion();
            }
            MagicUtil.poof(level, this.getX(), this.getY() + 1.0, this.getZ());
            for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(40))) {
                p.removeEffect(ModEffects.STAGE_FRIGHT);
                p.removeEffect(MobEffects.DARKNESS);
                p.displayClientMessage(Component.translatable("message.illusionist.curtain"), true);
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        for (ItemStack stack : stolen) {
            this.spawnAtLocation(stack);
        }
        stolen.clear();
    }

    // ------------------------------------------------------------------ прочее

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && !isIllusion() && this.random.nextInt(8) == 0) {
            this.level().addParticle(ParticleTypes.SMOKE, this.getRandomX(0.5), this.getRandomY(), this.getRandomZ(0.5), 0, 0.02, 0);
        }
    }

    @Override
    public boolean isPushable() {
        return !isIllusion() && super.isPushable();
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 1;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ILLUSIONER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ILLUSIONER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ILLUSIONER_DEATH;
    }
}
