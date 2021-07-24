package net.petersil98.utilcraft_building.network;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkEvent;
import net.petersil98.utilcraft_building.data.capabilities.blueprint.CapabilityBlueprint;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class SyncBlueprintTECapability {

    private final INBT data;
    private final BlockPos blockPos;

    public SyncBlueprintTECapability(@Nonnull BlockPos blockPos, World world) {
        AtomicReference<INBT> tag = new AtomicReference<>();
        world.getBlockEntity(blockPos).getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY).ifPresent(iBluePrint -> {
            tag.set(CapabilityBlueprint.BLUEPRINT_CAPABILITY.writeNBT(iBluePrint, null));
        });
        this.data = tag.get();
        this.blockPos = blockPos;
    }

    public SyncBlueprintTECapability(@Nonnull PacketBuffer packetBuffer) {
        this.data = packetBuffer.readNbt();
        this.blockPos = packetBuffer.readBlockPos();
    }

    public void encode(@Nonnull PacketBuffer buf) {
        buf.writeNbt((CompoundNBT) this.data);
        buf.writeBlockPos(this.blockPos);
    }

    public boolean handle(@Nonnull Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if(Minecraft.getInstance().level != null) {
                Minecraft.getInstance().level.getBlockEntity(this.blockPos).getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY).ifPresent(iBluePrint -> CapabilityBlueprint.BLUEPRINT_CAPABILITY.readNBT(iBluePrint, null, this.data));
            }
        });
        return true;
    }
}
