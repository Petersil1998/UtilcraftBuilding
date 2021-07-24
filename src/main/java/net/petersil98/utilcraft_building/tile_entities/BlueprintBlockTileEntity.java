package net.petersil98.utilcraft_building.tile_entities;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.petersil98.utilcraft_building.data.capabilities.blueprint.CapabilityBlueprint;
import net.petersil98.utilcraft_building.data.capabilities.blueprint.DefaultBlueprint;
import net.petersil98.utilcraft_building.data.capabilities.blueprint.IBluePrint;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlueprintBlockTileEntity extends TileEntity {

    private final DefaultBlueprint blueprint = new DefaultBlueprint();
    private final LazyOptional<IBluePrint> blueprintOptional = LazyOptional.of(() -> blueprint);

    public BlueprintBlockTileEntity() {
        super(UtilcraftBuildingTileEntities.BLUEPRINT_BLOCK);
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
    public CompoundNBT save(@Nonnull CompoundNBT compound) {
        super.save(compound);
        if (CapabilityBlueprint.BLUEPRINT_CAPABILITY != null) {
            compound.merge((CompoundNBT) CapabilityBlueprint.BLUEPRINT_CAPABILITY.writeNBT(blueprint, null));
        }
        return compound;
    }

    @Override
    public void load(@Nonnull BlockState state, @Nonnull CompoundNBT nbt) {
        super.load(state, nbt);
        if (CapabilityBlueprint.BLUEPRINT_CAPABILITY != null) {
            CapabilityBlueprint.BLUEPRINT_CAPABILITY.readNBT(this.blueprint, null, nbt);
        }
    }

    @Override
    protected void invalidateCaps() {
        super.invalidateCaps();
        this.blueprintOptional.invalidate();
    }

    @Nonnull
    @Override
    public CompoundNBT getUpdateTag() {
        CompoundNBT tag = super.getUpdateTag();
        tag.merge((CompoundNBT) CapabilityBlueprint.BLUEPRINT_CAPABILITY.writeNBT(this.blueprint, null));
        return tag;
    }

    @Override
    public void handleUpdateTag(BlockState state, CompoundNBT tag) {
        super.handleUpdateTag(state, tag);
        CapabilityBlueprint.BLUEPRINT_CAPABILITY.readNBT(this.blueprint, null, tag);
    }
}
