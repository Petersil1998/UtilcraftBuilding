package net.petersil98.utilcraft_building.data.capabilities.blueprint;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.petersil98.utilcraft_building.container.architect_table.ArchitectTableContainer;
import net.petersil98.utilcraft_building.data.capabilities.CapabilityStoreHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class BlueprintProvider implements ICapabilitySerializable<CompoundTag> {

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
    public CompoundTag serializeNBT() {
        IBluePrint instance = blueprintOptional.orElseThrow(() -> new IllegalArgumentException("Lazy optional is uninitialized"));
        return CapabilityStoreHelper.storeBlueprintCapability(instance);
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        IBluePrint instance = blueprintOptional.orElseThrow(() -> new IllegalArgumentException("Lazy optional is uninitialized"));
        CapabilityStoreHelper.readBlueprintCapability(nbt, instance);
    }
}