package com.illusionist.director;

import com.illusionist.Config;
import com.illusionist.IllusionistMod;
import com.illusionist.entity.CardProjectile;
import com.illusionist.entity.KindMagicianEntity;
import com.illusionist.entity.MagicianEntity;
import com.illusionist.registry.ModBlocks;
import com.illusionist.registry.ModEffects;
import com.illusionist.registry.ModEntities;
import com.illusionist.registry.ModItems;
import com.illusionist.registry.ModSounds;
import com.illusionist.util.DelayedTasks;
import com.illusionist.util.Screamer;
import com.illusionist.util.MagicUtil;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Режиссёр представления: у каждого игрока свой «акт» (I-IV) и шкала напряжения.
 *  Акт I   «Приглашение» — наблюдает, подкидывает странности, не нападает.
 *  Акт II  «Иллюзии и подмена» — карты, свет гаснет, иллюзии, кролики.
 *  Акт III «Смертельный номер» — охота в полную силу.
 *  Акт IV  «Финал» — максимальная жестокость.
 * Напряжение растёт ночью и в темноте, при заполнении акт сменяется. Идея и список событий: Gemini-версия мода.
 */
@EventBusSubscriber(modid = IllusionistMod.MODID)
public class PerformanceDirector {
    private static final String TAG_ACT = "illusionist_act";
    private static final String TAG_TENSION = "illusionist_tension";
    private static final String TAG_COOLDOWN = "illusionist_cooldown";
    private static final String TAG_BEDSIDE = "illusionist_bedside";

    private static final String[] WHISPERS = {
            "§4§o«Дамы и господа... представление начинается.»",
            "§4§o«Не отводи взгляд. Иначе пропустишь лучший фокус.»",
            "§4§o«Загляни в инвентарь... ничего не пропало?»",
            "§4§o«Угадай, кто из нас настоящий?»",
            "§4§o«Аплодисменты, пожалуйста! Занавес поднимается...»",
            "§4§o«Кролик уже выпрыгнул из шляпы. Обернись.»"
    };

    // ------------------------------------------------------------------ акты и напряжение

    public static int getAct(ServerPlayer player) {
        int act = player.getPersistentData().getInt(TAG_ACT);
        return act <= 0 ? 1 : Math.min(4, act);
    }

    public static void setAct(ServerPlayer player, int act) {
        player.getPersistentData().putInt(TAG_ACT, Math.max(1, Math.min(4, act)));
    }

    public static int getTension(ServerPlayer player) {
        return Math.max(0, Math.min(100, player.getPersistentData().getInt(TAG_TENSION)));
    }

    public static void addTension(ServerPlayer player, int delta) {
        CompoundTag data = player.getPersistentData();
        int next = Math.max(0, Math.min(100, data.getInt(TAG_TENSION) + delta));
        data.putInt(TAG_TENSION, next);
        int act = getAct(player);
        if (next >= 100 && act < 4) {
            setAct(player, act + 1);
            data.putInt(TAG_TENSION, 15);
            announceAct(player, act + 1);
        }
    }

    public static void announceAct(ServerPlayer player, int act) {
        String title = switch (act) {
            case 2 -> "§4[АКТ II: ИЛЛЮЗИИ И ПОДМЕНА]§7 Тени начинают шевелиться...";
            case 3 -> "§4[АКТ III: СМЕРТЕЛЬНЫЙ НОМЕР]§c Фокусник вышел на охоту!";
            case 4 -> "§4§l[АКТ IV: ФИНАЛ]§c Занавес закрывается. Выживи или раскрой обман!";
            default -> "§4[АКТ I: ПРИГЛАШЕНИЕ]§7 Кто-то наблюдает за тобой...";
        };
        player.displayClientMessage(Component.literal(title), false);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.BELL_RESONATE,
                SoundSource.HOSTILE, 1.2F, 0.55F);
    }

    /** Акт и напряжение переживают смерть игрока. */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        CompoundTag from = event.getOriginal().getPersistentData();
        CompoundTag to = event.getEntity().getPersistentData();
        for (String key : new String[]{TAG_ACT, TAG_TENSION}) {
            if (from.contains(key)) {
                to.putInt(key, from.getInt(key));
            }
        }
    }

    // ------------------------------------------------------------------ главный цикл

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !Config.DIRECTOR_ENABLED.get()) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        checkBedside(player);
        if (player.tickCount % 20 != 0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        CompoundTag data = player.getPersistentData();
        int cd = data.getInt(TAG_COOLDOWN);
        if (cd > 0) {
            data.putInt(TAG_COOLDOWN, cd - 1);
            return;
        }

        boolean dark = level.isNight() || level.getMaxLocalRawBrightness(player.blockPosition()) <= 6;
        RandomSource rng = level.random;
        boolean peaceful = level.getDifficulty() == Difficulty.PEACEFUL;
        if (dark && !peaceful && rng.nextFloat() < 0.16F) {
            addTension(player, 2);
        }

        // Днём — шанс странного, но мирного события
        if (!dark && rng.nextFloat() < 0.012F) {
            String[] dayEvents = {"kind_visit", "kind_visit", "roadside_hat", "card_rain", "phantom_footsteps"};
            String ev = dayEvents[rng.nextInt(dayEvents.length)];
            if (!ev.equals("kind_visit") || Config.KIND_VISITS.get()) {
                triggerEvent(player, ev);
                data.putInt(TAG_COOLDOWN, 55 + rng.nextInt(35));
            }
            return;
        }
        if (peaceful) {
            return;
        }
        int act = getAct(player);
        float chance = (dark ? 0.04F : 0.008F) * act;
        if (rng.nextFloat() < chance) {
            horrorBeat(player, act);
            data.putInt(TAG_COOLDOWN, Math.max(25, 80 - act * 13) + rng.nextInt(25));
        }
    }

    /** «Гость у кровати»: уснувшего игрока (акт II+) иногда будит неподвижная фигура рядом. */
    private static void checkBedside(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!player.isSleeping()) {
            data.putBoolean(TAG_BEDSIDE, false);
            return;
        }
        if (data.getBoolean(TAG_BEDSIDE) || player.getSleepTimer() < 40) {
            return;
        }
        data.putBoolean(TAG_BEDSIDE, true);
        ServerLevel level = player.serverLevel();
        if (getAct(player) < 2 || level.random.nextFloat() > 0.4F || level.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        triggerEvent(player, "bedside");
    }

    private static boolean magicianNearby(ServerLevel level, ServerPlayer player) {
        return !level.getEntitiesOfClass(MagicianEntity.class, player.getBoundingBox().inflate(48.0),
                m -> !m.isIllusion()).isEmpty();
    }

    private static void horrorBeat(ServerPlayer player, int act) {
        ServerLevel level = player.serverLevel();
        RandomSource rng = level.random;
        boolean exists = magicianNearby(level, player);
        if (rng.nextInt(8) == 0) {
            String[] quiet = {"ghost_door", "distant_lullaby", "fake_join"};
            triggerEvent(player, quiet[rng.nextInt(quiet.length)]);
            addTension(player, 6);
            return;
        }
        if (rng.nextInt(7) == 0 && ImpostorHandler.spawnImpostor(player)) {
            addTension(player, 8);
            return;
        }

        if (act == 1) {
            switch (rng.nextInt(6)) {
                case 0 -> giveTicketOnce(player);
                case 1 -> triggerEvent(player, "phantom_footsteps");
                case 2 -> triggerEvent(player, "door_knock");
                case 3 -> triggerEvent(player, "kind_visit");
                case 4 -> triggerEvent(player, "roadside_hat");
                default -> {
                    if (!exists) spawnMagicianNear(player, false);
                }
            }
            addTension(player, 10);
        } else if (act == 2) {
            switch (rng.nextInt(7)) {
                case 0 -> triggerTrick(player, "shuffle");
                case 1 -> triggerTrick(player, "rabbits");
                case 2 -> triggerEvent(player, "torch_swap");
                case 3 -> triggerEvent(player, "phantom_applause");
                case 4 -> triggerEvent(player, "baited_gift");
                case 5 -> triggerEvent(player, rng.nextInt(3) == 0 ? "screamer" : "watcher");
                default -> {
                    if (!exists) spawnMagicianNear(player, false);
                }
            }
            addTension(player, 12);
        } else {
            if (!exists) {
                spawnMagicianNear(player, true);
            } else {
                switch (rng.nextInt(7)) {
                    case 0 -> triggerTrick(player, "clones");
                    case 1 -> triggerTrick(player, "curtain");
                    case 2 -> triggerTrick(player, "rabbits");
                    case 3 -> triggerTrick(player, "shuffle");
                    case 4 -> triggerTrick(player, "cards");
                    case 5 -> triggerEvent(player, "phantom_applause");
                    default -> triggerEvent(player, "baited_gift");
                }
            }
            addTension(player, 10);
        }
    }

    private static void giveTicketOnce(ServerPlayer player) {
        if (player.getInventory().contains(new ItemStack(ModItems.SHOW_TICKET.get()))) {
            return;
        }
        if (player.getInventory().add(new ItemStack(ModItems.SHOW_TICKET.get()))) {
            player.displayClientMessage(Component.literal("§4§oВ кармане сам собой появился алый билет..."), true);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                    SoundSource.AMBIENT, 0.8F, 0.5F);
        }
    }

    // ------------------------------------------------------------------ события мира

    public static final String[] EVENTS = {"kind_visit", "phantom_footsteps", "door_knock", "card_rain",
            "roadside_hat", "phantom_applause", "baited_gift", "torch_swap", "screamer", "watcher", "impostor", "ghost_door", "bedside", "distant_lullaby", "fake_join"};
    public static final String[] TRICKS = {"shuffle", "clones", "rabbits", "curtain", "cards", "vanish"};

    public static void triggerEvent(ServerPlayer player, String name) {
        ServerLevel level = player.serverLevel();
        RandomSource rng = level.random;
        Vec3 flat = flatLook(player);

        switch (name) {
            case "kind_visit" -> spawnKindMagicianNear(player);

            case "phantom_footsteps" -> {
                Vec3 behind = player.position().subtract(flat.scale(3.5D));
                BlockPos pos = BlockPos.containing(behind.x, player.getY(), behind.z);
                level.playSound(null, pos, SoundEvents.GRASS_STEP, SoundSource.HOSTILE, 1.2F, 0.7F);
                level.playSound(null, pos, SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 0.85F, 0.75F);
                level.addFreshEntity(new ItemEntity(level, behind.x, player.getY() + 0.3, behind.z,
                        new ItemStack(ModItems.MAGIC_CARD.get())));
                level.sendParticles(ParticleTypes.SMOKE, behind.x, player.getY() + 0.5, behind.z, 14, 0.3, 0.4, 0.3, 0.02);
                player.displayClientMessage(Component.literal("§4§oСзади послышались быстрые шаги... На земле лежит одинокая карта."), true);
            }

            case "door_knock" -> {
                BlockPos p = player.blockPosition();
                level.playSound(null, p.north(2), SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE, 0.85F, 0.8F);
                level.playSound(null, p.south(2), SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.HOSTILE, 0.9F, 0.6F);
                player.addEffect(new MobEffectInstance(ModEffects.STAGE_FRIGHT, 90, 0));
                player.displayClientMessage(Component.literal("§4§o*Тук... тук... тук...* «Впусти артиста на минутку.»"), true);
            }

            case "card_rain" -> {
                for (int i = 0; i < 12; i++) {
                    double ox = player.getX() + (rng.nextDouble() - 0.5) * 9.0;
                    double oz = player.getZ() + (rng.nextDouble() - 0.5) * 9.0;
                    double oy = player.getY() + 6.0 + rng.nextDouble() * 4.0;
                    if (i < 5) {
                        level.addFreshEntity(new ItemEntity(level, ox, oy, oz, new ItemStack(ModItems.MAGIC_CARD.get())));
                    } else {
                        CardProjectile card = new CardProjectile(ModEntities.CARD.get(), level);
                        card.setPos(ox, oy, oz);
                        card.setNoGravity(false);
                        card.shoot(0.0, -0.55, 0.0, 0.7F, 6.0F);
                        level.addFreshEntity(card);
                    }
                }
                level.sendParticles(ParticleTypes.FIREWORK, player.getX(), player.getY() + 5.0, player.getZ(), 30, 3.0, 1.0, 3.0, 0.06);
                level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.HOSTILE, 1.0F, 1.4F);
                player.displayClientMessage(Component.literal("§4§oС неба посыпался дождь из игральных карт!"), true);
            }

            case "roadside_hat" -> {
                Vec3 ahead = player.position().add(flat.scale(5.5D));
                BlockPos pos = MagicUtil.findStandable(level, ahead.x, player.getY(), ahead.z);
                if (pos != null) {
                    level.setBlock(pos, ModBlocks.CURSED_TOP_HAT.get().defaultBlockState(), 3);
                    level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 22, 0.4, 0.5, 0.4, 0.04);
                    level.playSound(null, pos, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 0.9F, 0.75F);
                    player.displayClientMessage(Component.literal("§4§oПрямо на твоём пути кто-то оставил чёрный цилиндр..."), true);
                }
            }

            case "phantom_applause" -> {
                for (int i = 0; i < 6; i++) {
                    double angle = (Math.PI * 2.0 / 6.0) * i;
                    level.sendParticles(ParticleTypes.FLAME, player.getX() + Math.cos(angle) * 6.0, player.getY() + 1.6,
                            player.getZ() + Math.sin(angle) * 6.0, 6, 0.1, 0.2, 0.1, 0.01);
                }
                level.playSound(null, player.blockPosition(), ModSounds.APPLAUSE.get(), SoundSource.HOSTILE, 1.6F, 0.85F);
                player.addEffect(new MobEffectInstance(ModEffects.STAGE_FRIGHT, 120, 0));
                player.displayClientMessage(Component.literal("§4§oИз темноты вокруг раздаются призрачные аплодисменты... Браво!"), true);
            }

            case "baited_gift" -> {
                Vec3 ahead = player.position().add(flat.scale(4.5D));
                level.addFreshEntity(new ItemEntity(level, ahead.x, player.getY() + 0.4, ahead.z, new ItemStack(Items.EMERALD)));
                MagicianEntity.spawnIllusion(level, ahead.x + 1.5, player.getY(), ahead.z + 1.5, player);
                level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.HOSTILE, 0.9F, 0.5F);
                player.displayClientMessage(Component.literal("§4§oБлестящий изумруд на дороге... но это приманка Фокусника!"), true);
            }

            case "torch_swap" -> {
                MagicUtil.snuffLights(level, player.blockPosition(), 6);
                level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 1.0F, 0.55F);
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
                player.displayClientMessage(Component.literal("§4§oОгни вокруг внезапно погасли..."), true);
            }
            case "impostor" -> ImpostorHandler.spawnImpostor(player);

            case "ghost_door" -> DoorHaunt.slowOpen(player);

            case "distant_lullaby" -> {
                double ang = rng.nextDouble() * Math.PI * 2.0D;
                BlockPos at = BlockPos.containing(player.getX() + Math.cos(ang) * 20.0D, player.getY(),
                        player.getZ() + Math.sin(ang) * 20.0D);
                float[] notes = {1.00F, 1.19F, 1.50F, 1.19F, 1.00F, 0.89F, 0.84F, 0.89F};
                for (int i = 0; i < notes.length; i++) {
                    final float pitch = notes[i] * 0.8F;
                    DelayedTasks.schedule(level, 14 * i, () ->
                            level.playSound(null, at, ModSounds.MUSIC_BOX_NOTE.get(), SoundSource.HOSTILE, 3.0F, pitch));
                }
                DelayedTasks.schedule(level, 14 * notes.length + 10, () ->
                        player.displayClientMessage(Component.literal("§4§oМелодия оборвалась на полуноте."), true));
            }

            case "fake_join" -> {
                player.sendSystemMessage(Component.literal("§eФокусник присоединился к игре"));
                DelayedTasks.schedule(level, 60, () ->
                        player.sendSystemMessage(Component.literal("<Фокусник> Не оборачивайся.")));
                DelayedTasks.schedule(level, 110, () ->
                        player.sendSystemMessage(Component.literal("§eФокусник покинул игру")));
            }

            case "bedside" -> {
                player.stopSleeping();
                Vec3 side = player.position().add(flat.scale(2.2D));
                MagicianEntity w = MagicianEntity.spawnIllusion(level, side.x, player.getY(), side.z, null);
                level.playSound(null, player.blockPosition(), ModSounds.LAUGH.get(), SoundSource.HOSTILE, 0.8F, 0.6F);
                player.addEffect(new MobEffectInstance(ModEffects.STAGE_FRIGHT, 200, 0));
                if (w != null) {
                    player.displayClientMessage(Component.literal("§4§oВы проснулись. У кровати кто-то стоял... и смотрел."), true);
                    Screamer.trigger(player);
                }
            }

            case "screamer" -> {
                level.playSound(null, player.blockPosition(), ModSounds.LAUGH.get(), SoundSource.HOSTILE, 1.2F, 0.7F);
                Screamer.trigger(player, true);
            }

            case "watcher" -> {
                // Далёкая неподвижная фигура в цилиндре. Стоит посмотреть — исчезает.
                Vec3 far = player.position().add(flat.scale(16.0D + rng.nextInt(7)));
                MagicianEntity w = MagicianEntity.spawnIllusion(level, far.x, player.getY(), far.z, null);
                if (w != null) {
                    player.displayClientMessage(Component.literal("§4§oВдали кто-то стоит. Неподвижно. В цилиндре."), true);
                }
            }
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ «номера»

    public static void triggerTrick(ServerPlayer player, String trick) {
        ServerLevel level = player.serverLevel();
        RandomSource rng = level.random;

        switch (trick) {
            case "shuffle" -> {
                if (com.illusionist.item.MonocleOfTruthItem.isProtected(player)) {
                    player.displayClientMessage(Component.literal("§b[Монокль Истины]§r Иллюзия подмены хотбара рассеяна!"), true);
                    return;
                }
                Inventory inv = player.getInventory();
                for (int i = 0; i < 9; i++) {
                    int swap = rng.nextInt(9);
                    ItemStack a = inv.getItem(i).copy();
                    ItemStack b = inv.getItem(swap).copy();
                    inv.setItem(i, b);
                    inv.setItem(swap, a);
                }
                level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.HOSTILE, 1.0F, 1.3F);
                player.displayClientMessage(Component.literal("§4§oФокус «Ловкость рук»! Ваши предметы перетасованы..."), true);
            }
            case "clones" -> {
                for (int i = 0; i < 3; i++) {
                    double angle = (Math.PI * 2.0 / 3.0) * i + rng.nextDouble() * 0.5;
                    MagicianEntity.spawnIllusion(level, player.getX() + Math.cos(angle) * 5.5, player.getY(),
                            player.getZ() + Math.sin(angle) * 5.5, player);
                }
                level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 1.1F, 0.7F);
                player.displayClientMessage(Component.literal("§4§oФокус «Зеркальный зал»! Угадай, кто настоящий..."), true);
            }
            case "rabbits" -> {
                for (int i = 0; i < 2; i++) {
                    MagicUtil.spawnHatRabbit(level, player.getX() + (rng.nextDouble() - 0.5) * 6.0, player.getY() + 0.5,
                            player.getZ() + (rng.nextDouble() - 0.5) * 6.0, player);
                }
                level.playSound(null, player.blockPosition(), SoundEvents.VEX_CHARGE, SoundSource.HOSTILE, 1.0F, 0.5F);
                player.displayClientMessage(Component.literal("§4§oФокус «Кролик из шляпы»!"), true);
            }
            case "curtain" -> {
                BlockPos center = player.blockPosition();
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                            for (int dy = 0; dy <= 2; dy++) {
                                BlockPos p = center.offset(dx, dy, dz);
                                if (level.getBlockState(p).isAir()) {
                                    level.setBlock(p, ModBlocks.PHANTOM_CURTAIN.get().defaultBlockState(), 3);
                                }
                            }
                        }
                    }
                }
                player.addEffect(new MobEffectInstance(ModEffects.STAGE_FRIGHT, 160, 0));
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
                level.playSound(null, center, SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 0.7F, 0.65F);
                player.displayClientMessage(Component.literal("§4§oЗанавес опускается..."), true);
            }
            case "cards" -> {
                Vec3 origin = player.getEyePosition().add(player.getLookAngle().scale(6.0));
                for (int i = -2; i <= 2; i++) {
                    CardProjectile card = new CardProjectile(ModEntities.CARD.get(), level);
                    card.setPos(origin.x, origin.y + 0.5, origin.z);
                    Vec3 dir = player.getEyePosition().subtract(origin).normalize().yRot(i * 0.14F);
                    card.shoot(dir.x, dir.y, dir.z, 1.25F, 1.0F);
                    level.addFreshEntity(card);
                }
                level.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 0.8F, 1.5F);
            }
            case "vanish" -> {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 55, 0));
                player.addEffect(new MobEffectInstance(ModEffects.STAGE_FRIGHT, 140, 0));
                level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.55F);
            }
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ появление

    private static Vec3 flatLook(ServerPlayer player) {
        Vec3 flat = player.getLookAngle().multiply(1, 0, 1);
        return flat.lengthSqr() < 0.01D ? new Vec3(1, 0, 0) : flat.normalize();
    }

    public static void spawnKindMagicianNear(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 ahead = player.position().add(flatLook(player).scale(5.5D));
        BlockPos pos = MagicUtil.findStandable(level, ahead.x, player.getY(), ahead.z);
        if (pos == null) {
            return;
        }
        KindMagicianEntity kind = ModEntities.KIND_MAGICIAN.get().create(level);
        if (kind == null) {
            return;
        }
        kind.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot() + 180.0F, 0.0F);
        level.addFreshEntity(kind);
        level.sendParticles(ParticleTypes.FIREWORK, kind.getX(), kind.getY() + 1.4, kind.getZ(), 35, 0.6, 1.0, 0.6, 0.08);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.2F, 1.15F);
        player.displayClientMessage(Component.literal(
                "§6[Добрый Фокусник]§f Приветствую, зритель! Нажми по мне §eПКМ§f, и я покажу тебе настоящий добрый фокус!"), false);
    }

    /** Настоящий Фокусник рядом с игроком. aggressive — ближе и сразу в атаку. */
    @Nullable
    public static MagicianEntity spawnMagicianNear(ServerPlayer player, boolean aggressive) {
        ServerLevel level = player.serverLevel();
        RandomSource rng = level.random;
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = rng.nextDouble() * Math.PI * 2.0;
            double dist = aggressive ? (10.0 + rng.nextDouble() * 6.0) : (18.0 + rng.nextDouble() * 10.0);
            BlockPos pos = MagicUtil.findStandable(level, player.getX() + Math.cos(angle) * dist, player.getY(),
                    player.getZ() + Math.sin(angle) * dist);
            if (pos == null) {
                continue;
            }
            MagicianEntity magician = ModEntities.MAGICIAN.get().create(level);
            if (magician == null) {
                return null;
            }
            magician.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, rng.nextFloat() * 360.0F, 0.0F);
            if (aggressive) {
                magician.setTarget(player);
            }
            magician.setPersistenceRequired();
            level.addFreshEntity(magician);
            MagicUtil.poof(level, magician.getX(), magician.getY() + 1.3, magician.getZ());
            level.playSound(null, pos, SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 1.3F, 0.6F);
            player.displayClientMessage(Component.literal(WHISPERS[rng.nextInt(WHISPERS.length)]), true);
            return magician;
        }
        return null;
    }

    public static List<String> names(String[] arr) {
        return List.of(arr);
    }
}
