package net.petersil98.utilcraft_building.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.petersil98.utilcraft_building.container.architect_table.ArchitectTableContainer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ArchitectTable extends Block {

    public ArchitectTable(Properties properties) {
        super(properties);
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public ActionResultType use(@Nonnull BlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull PlayerEntity player, @Nonnull Hand hand, @Nonnull BlockRayTraceResult hit) {
        if (world.isClientSide) {
            return ActionResultType.SUCCESS;
        } else {
            player.openMenu(state.getMenuProvider(world, pos));
            return ActionResultType.CONSUME;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean useShapeForLightOcclusion(@Nonnull BlockState state) {
        return true;
    }

    @Nullable
    @SuppressWarnings("deprecation")
    public INamedContainerProvider getMenuProvider(@Nonnull BlockState state, @Nonnull World world, @Nonnull BlockPos pos) {
        return new SimpleNamedContainerProvider((id, inventory, player) -> new ArchitectTableContainer(id, inventory, IWorldPosCallable.create(world, pos)), new TranslationTextComponent(getDescriptionId()));
    }
}
