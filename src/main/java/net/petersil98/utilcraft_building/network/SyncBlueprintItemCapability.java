package net.petersil98.utilcraft_building.network;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.petersil98.utilcraft_building.data.capabilities.blueprint.CapabilityBlueprint;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class SyncBlueprintItemCapability {

    private final INBT data;
    private final ItemStack itemStack;

    public SyncBlueprintItemCapability(@Nonnull ItemStack itemStack) {
        AtomicReference<INBT> tag = new AtomicReference<>();
        itemStack.getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY).ifPresent(iBluePrint -> {
            tag.set(CapabilityBlueprint.BLUEPRINT_CAPABILITY.writeNBT(iBluePrint, null));
        });
        this.data = tag.get();
        this.itemStack = itemStack;
    }

    public SyncBlueprintItemCapability(@Nonnull PacketBuffer packetBuffer) {
        this.data = packetBuffer.readNbt();
        this.itemStack = packetBuffer.readItem();
    }

    public void encode(@Nonnull PacketBuffer buf) {
        buf.writeNbt((CompoundNBT) this.data);
        buf.writeItem(this.itemStack);
    }

    public boolean handle(@Nonnull Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> this.itemStack.getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY).ifPresent(iBluePrint -> CapabilityBlueprint.BLUEPRINT_CAPABILITY.readNBT(iBluePrint, null, this.data)));
        return true;
    }
}
