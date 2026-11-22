package com.illusionist.registry;

import com.illusionist.IllusionistMod;
import com.illusionist.block.CursedTopHatBlock;
import com.illusionist.block.PhantomCurtainBlock;
import com.illusionist.block.TrickBoxBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(IllusionistMod.MODID);

    public static final DeferredBlock<TrickBoxBlock> TRICK_BOX = BLOCKS.register("trick_box",
            () -> new TrickBoxBlock(BlockBehaviour.Properties.of()
                    .strength(1.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final DeferredBlock<CursedTopHatBlock> CURSED_TOP_HAT = BLOCKS.register("cursed_top_hat",
            () -> new CursedTopHatBlock(BlockBehaviour.Properties.of()
                    .strength(0.8F)
                    .sound(SoundType.WOOL)
                    .noOcclusion()
                    .randomTicks()));

    public static final DeferredBlock<PhantomCurtainBlock> PHANTOM_CURTAIN = BLOCKS.register("phantom_curtain",
            () -> new PhantomCurtainBlock(BlockBehaviour.Properties.of()
                    .strength(0.3F)
                    .sound(SoundType.WOOL)
                    .noCollission()
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY)));
}
