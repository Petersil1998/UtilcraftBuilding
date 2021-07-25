package net.petersil98.utilcraft_building.block_entities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.petersil98.utilcraft_building.data.capabilities.CapabilityStoreHelper;
import net.petersil98.utilcraft_building.data.capabilities.blueprint.CapabilityBlueprint;
import net.petersil98.utilcraft_building.data.capabilities.blueprint.DefaultBlueprint;
import net.petersil98.utilcraft_building.data.capabilities.blueprint.IBluePrint;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlueprintBlockEntity extends BlockEntity {

    private final DefaultBlueprint blueprint = new DefaultBlueprint();
    private final LazyOptional<IBluePrint> blueprintOptional = LazyOptional.of(() -> blueprint);

    public BlueprintBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(UtilcraftBuildingBlockEntities.BLUEPRINT_BLOCK, blockPos, blockState);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if(cap == CapabilityBlueprint.BLUEPRINT_CAPABILITY) {
            return this.blueprintOptional.cast();
        } else {
            return LazyOptional.empty();
        }
    }

    @Nonnull
    @Override
    public CompoundTag save(@Nonnull CompoundTag compound) {
        super.save(compound);
        compound.merge(CapabilityStoreHelper.storeBlueprintCapability(this.blueprintOptional.orElseThrow(() ->  new IllegalArgumentException("Lazy optional is uninitialized"))));
        return compound;
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
        CapabilityStoreHelper.readBlueprintCapability(nbt, this.blueprintOptional.orElseThrow(() ->  new IllegalArgumentException("Lazy optional is uninitialized")));
    }

    @Override
    protected void invalidateCaps() {
        super.invalidateCaps();
        this.blueprintOptional.invalidate();
    }

    @Nonnull
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.merge(CapabilityStoreHelper.storeBlueprintCapability(this.blueprintOptional.orElseThrow(() ->  new IllegalArgumentException("Lazy optional is uninitialized"))));
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag nbt) {
        super.handleUpdateTag(nbt);
        CapabilityStoreHelper.readBlueprintCapability(nbt, this.blueprintOptional.orElseThrow(() ->  new IllegalArgumentException("Lazy optional is uninitialized")));
    }
}
