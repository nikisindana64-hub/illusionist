package com.illusionist.block;

import com.illusionist.registry.ModEffects;
import com.illusionist.registry.ModItems;
import com.illusionist.util.DelayedTasks;
import com.illusionist.util.MagicUtil;
import com.illusionist.util.Screamer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Игра в напёрстки. Три стакана на столике. ПКМ — перетасовать (шарик прячется под случайный стакан),
 * затем ПКМ по стакану, под которым, по-вашему, шарик. Угадали — приз. Нет — расплата.
 * BALL: 0-2 = шарик под этим стаканом (игра идёт), 3 = стаканы не тасовались.
 */
public class ShellGameBlock extends Block {
    public static final IntegerProperty BALL = IntegerProperty.create("ball", 0, 3);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 11, 16);

    public ShellGameBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(BALL, 3));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BALL);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        int ball = state.getValue(BALL);
        if (ball == 3) {
            server.setBlock(pos, state.setValue(BALL, server.random.nextInt(3)), 3);
            for (int i = 0; i < 6; i++) {
                final float pitch = 0.7F + 0.15F * (i % 3);
                DelayedTasks.schedule(server, i * 4, () ->
                        server.playSound(null, pos, SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.7F, pitch));
            }
            player.displayClientMessage(Component.translatable("block.illusionist.cup_table.shuffled"), true);
            return InteractionResult.CONSUME;
        }
        double x = hit.getLocation().x - pos.getX();
        int pick = x < 0.34D ? 0 : (x < 0.66D ? 1 : 2);
        server.setBlock(pos, state.setValue(BALL, 3), 3);
        double px = pos.getX() + 0.5D;
        double py = pos.getY() + 1.0D;
        double pz = pos.getZ() + 0.5D;
        if (pick == ball) {
            ItemStack[] prizes = {
                    new ItemStack(Items.EMERALD, 1 + server.random.nextInt(3)),
                    new ItemStack(Items.GOLD_NUGGET, 6 + server.random.nextInt(8)),
                    new ItemStack(ModItems.MAGIC_CARD.get(), 6 + server.random.nextInt(8)),
                    new ItemStack(ModItems.SMOKE_BOMB.get(), 2),
                    new ItemStack(ModItems.LUCKY_COIN.get(), 1)
            };
            Block.popResource(server, pos.above(), prizes[server.random.nextInt(prizes.length)]);
            server.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.7F, 1.4F);
            player.displayClientMessage(Component.translatable("block.illusionist.cup_table.win"), true);
        } else {
            MagicUtil.poof(server, px, py, pz);
            player.displayClientMessage(Component.translatable("block.illusionist.cup_table.lose"), true);
            int roll = server.random.nextInt(10);
            if (roll < 4) {
                MagicUtil.spawnHatRabbit(server, px, py, pz, player);
            } else if (roll < 8) {
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
                player.addEffect(new MobEffectInstance(ModEffects.STAGE_FRIGHT, 300, 0));
            } else if (player instanceof ServerPlayer sp) {
                Screamer.trigger(sp, true);
            }
        }
        return InteractionResult.CONSUME;
    }
}
