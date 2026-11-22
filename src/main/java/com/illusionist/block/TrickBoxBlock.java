package com.illusionist.block;

import com.illusionist.entity.MagicianEntity;
import com.illusionist.registry.ModEffects;
import com.illusionist.registry.ModItems;
import com.illusionist.registry.ModSounds;
import com.illusionist.util.MagicUtil;
import com.illusionist.util.Screamer;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Шкатулка с сюрпризом (jack-in-the-box).
 * ПКМ: заводится музыкальная шкатулка. Мелодия замедляется... и крышка открывается.
 * Фазы: 0 закрыта, 1-5 играет мелодия, 6 тишина (тревожная пауза), 7 открыта.
 */
public class TrickBoxBlock extends Block {
    public static final IntegerProperty PHASE = IntegerProperty.create("phase", 0, 7);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 12, 14);
    private static final float[] NOTES = {1.0F, 1.19F, 1.5F, 1.33F, 0.89F};

    public TrickBoxBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(PHASE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PHASE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        int phase = state.getValue(PHASE);
        if (phase == 0) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(PHASE, 1), 3);
                level.scheduleTick(pos, this, 10);
                level.playSound(null, pos, SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.8F, 0.6F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (phase == 7) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(PHASE, 0), 3);
                level.playSound(null, pos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 1.0F, 0.8F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int phase = state.getValue(PHASE);
        if (phase >= 1 && phase <= 5) {
            level.playSound(null, pos, ModSounds.MUSIC_BOX_NOTE.get(), SoundSource.BLOCKS, 2.0F, NOTES[phase - 1]);
            level.sendParticles(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                    1, 0.2, 0.1, 0.2, 1.0);
            level.setBlock(pos, state.setValue(PHASE, phase + 1), 3);
            if (phase == 5) {
                level.playSound(null, pos, ModSounds.DRUMROLL.get(), SoundSource.BLOCKS, 1.6F, 1.0F);
            }
            level.scheduleTick(pos, this, phase == 5 ? 55 : 9 + phase);
        } else if (phase == 6) {
            pop(state, level, pos, random);
        }
    }

    private void pop(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.setBlock(pos, state.setValue(PHASE, 7), 3);
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.0;
        double z = pos.getZ() + 0.5;
        level.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 1.2F, 0.5F);
        level.playSound(null, pos, SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 0.7F, 1.4F);
        MagicUtil.poof(level, x, y, z);

        int roll = random.nextInt(100);
        if (roll < 35) {
            // Подарок
            ItemStack[] prizes = {
                    new ItemStack(ModItems.MAGIC_CARD.get(), 4 + random.nextInt(8)),
                    new ItemStack(ModItems.SMOKE_BOMB.get(), 1 + random.nextInt(2)),
                    new ItemStack(ModItems.JOKER_CARD.get(), 1),
                    new ItemStack(Items.GOLD_NUGGET, 3 + random.nextInt(6))
            };
            Block.popResource(level, pos.above(), prizes[random.nextInt(prizes.length)]);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 0.3, z, 10, 0.3, 0.3, 0.3, 0.0);
        } else if (roll < 70) {
            // Кролик-убийца
            List<Player> players = level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(10));
            MagicUtil.spawnHatRabbit(level, x, y, z, players.isEmpty() ? null : players.get(0));
        } else if (roll < 90) {
            // Страх
            for (Player p : level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(8))) {
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
                p.addEffect(new MobEffectInstance(ModEffects.STAGE_FRIGHT, 400, 0));
                if (p instanceof net.minecraft.server.level.ServerPlayer sp) {
                    Screamer.trigger(sp, true);
                }
            }
        } else {
            // Хозяин пришёл
            List<Player> players = level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(16));
            if (!players.isEmpty()) {
                MagicianEntity.summonNear(level, players.get(0));
            }
        }
    }
}
