package net.petersil98.utilcraft_building.blocks;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import net.petersil98.utilcraft_building.container.architect_table.ArchitectTableContainer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ArchitectTable extends Block {

    public ArchitectTable() {
        super(BlockBehaviour.Properties
                .of(Material.STONE)
                .requiresCorrectToolForDrops()
                .strength(3.5F)
                .noOcclusion());
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level world, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            player.openMenu(state.getMenuProvider(world, pos));
            return InteractionResult.CONSUME;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean useShapeForLightOcclusion(@Nonnull BlockState state) {
        return true;
    }

    @Nullable
    @SuppressWarnings("deprecation")
    public MenuProvider getMenuProvider(@Nonnull BlockState state, @Nonnull Level world, @Nonnull BlockPos pos) {
        return new SimpleMenuProvider((id, inventory, player) -> new ArchitectTableContainer(id, inventory, ContainerLevelAccess.create(world, pos)), getName());
    }
}
