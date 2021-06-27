package net.petersil98.utilcraft_building.blocks;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;
import net.petersil98.utilcraft_building.container.BlueprintBlockContainer;
import net.petersil98.utilcraft_building.tile_entities.BlueprintBlockTileEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlueprintBlock extends Block {

    public static final DirectionProperty FACING = HorizontalBlock.HORIZONTAL_FACING;

    public BlueprintBlock() {
        super(AbstractBlock.Properties
                .create(Material.ROCK)
                .setRequiresTool()
                .hardnessAndResistance(3.5F)
                .noDrops()
                .doesNotBlockMovement()
        );
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new BlueprintBlockTileEntity();
    }

    @Override
    public BlockState getStateForPlacement(@Nonnull BlockItemUseContext context) {
        return this.getDefaultState().with(FACING, context.getPlacementHorizontalFacing());
    }

    @Override
    protected void fillStateContainer(@Nonnull StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public ActionResultType onBlockActivated(@Nonnull BlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull PlayerEntity player, @Nonnull Hand hand, @Nonnull BlockRayTraceResult hit) {
        if (world.isRemote) {
            return ActionResultType.SUCCESS;
        } else {
            NetworkHooks.openGui((ServerPlayerEntity) player, state.getContainer(world,pos), pos);
            return ActionResultType.CONSUME;
        }
    }

    @Nullable
    @SuppressWarnings("deprecation")
    public INamedContainerProvider getContainer(@Nonnull BlockState state, @Nonnull World world, @Nonnull BlockPos pos) {
        return new SimpleNamedContainerProvider((id, inventory, player) -> new BlueprintBlockContainer(id, inventory, IWorldPosCallable.of(world, pos)), getTranslatedName());
    }
}
