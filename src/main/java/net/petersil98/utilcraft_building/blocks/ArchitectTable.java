package net.petersil98.utilcraft_building.blocks;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;
import net.petersil98.utilcraft_building.container.ArchitectTableContainer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ArchitectTable extends Block {

    public ArchitectTable() {
        super(AbstractBlock.Properties
                .create(Material.ROCK)
                .setRequiresTool()
                .hardnessAndResistance(3.5F)
                .notSolid());
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public ActionResultType onBlockActivated(@Nonnull BlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull PlayerEntity player, @Nonnull Hand hand, @Nonnull BlockRayTraceResult hit) {
        if (world.isRemote) {
            return ActionResultType.SUCCESS;
        } else {
            player.openContainer(state.getContainer(world, pos));
            return ActionResultType.CONSUME;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isTransparent(@Nonnull BlockState state) {
        return true;
    }

    @Nullable
    @SuppressWarnings("deprecation")
    public INamedContainerProvider getContainer(@Nonnull BlockState state, @Nonnull World world, @Nonnull BlockPos pos) {
        return new SimpleNamedContainerProvider((id, inventory, player) -> new ArchitectTableContainer(id, inventory, IWorldPosCallable.of(world, pos)), getTranslatedName());
    }
}
