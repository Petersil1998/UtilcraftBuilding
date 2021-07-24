package net.petersil98.utilcraft_building.data.capabilities.blueprint;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlueprintProvider implements ICapabilitySerializable<CompoundNBT> {

    private final DefaultBlueprint blueprint = new DefaultBlueprint();
    private final LazyOptional<IBluePrint> blueprintOptional = LazyOptional.of(() -> blueprint);

    public void invalidate() {
        blueprintOptional.invalidate();
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if(cap == CapabilityBlueprint.BLUEPRINT_CAPABILITY) {
            return blueprintOptional.cast();
        } else {
            return LazyOptional.empty();
        }
    }

    @Override
    public CompoundNBT serializeNBT() {
        return (CompoundNBT) CapabilityBlueprint.BLUEPRINT_CAPABILITY.writeNBT(blueprint, null);
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        CapabilityBlueprint.BLUEPRINT_CAPABILITY.readNBT(blueprint, null, nbt);
    }
}