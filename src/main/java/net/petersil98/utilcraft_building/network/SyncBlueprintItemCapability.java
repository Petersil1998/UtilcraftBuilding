package net.petersil98.utilcraft_building.network;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.petersil98.utilcraft_building.data.capabilities.blueprint.CapabilityBlueprint;
import net.petersil98.utilcraft_building.screen.ArchitectTableScreen;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class SyncBlueprintItemCapability {

    private final INBT data;

    public SyncBlueprintItemCapability(@Nonnull ItemStack itemStack) {
        AtomicReference<INBT> tag = new AtomicReference<>();
        itemStack.getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY).ifPresent(iBluePrint -> {
            tag.set(CapabilityBlueprint.BLUEPRINT_CAPABILITY.writeNBT(iBluePrint, null));
        });
        this.data = tag.get();
    }

    public SyncBlueprintItemCapability(@Nonnull PacketBuffer packetBuffer) {
        this.data = packetBuffer.readNbt();
    }

    public void encode(@Nonnull PacketBuffer buf) {
        buf.writeNbt((CompoundNBT) this.data);
    }

    public boolean handle(@Nonnull Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if(Minecraft.getInstance().screen instanceof ArchitectTableScreen) {
                ArchitectTableScreen screen = (ArchitectTableScreen) Minecraft.getInstance().screen;
                screen.getMenu().getBlueprint().getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY).ifPresent(iBluePrint -> CapabilityBlueprint.BLUEPRINT_CAPABILITY.readNBT(iBluePrint, null, this.data));
            }
        });
        return true;
    }
}
