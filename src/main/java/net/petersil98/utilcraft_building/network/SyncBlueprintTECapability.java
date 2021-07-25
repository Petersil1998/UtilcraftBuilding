package net.petersil98.utilcraft_building.network;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.fmllegacy.network.NetworkEvent;
import net.petersil98.utilcraft_building.data.capabilities.CapabilityStoreHelper;
import net.petersil98.utilcraft_building.data.capabilities.blueprint.CapabilityBlueprint;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class SyncBlueprintTECapability {

    private final CompoundTag data;
    private final BlockPos blockPos;

    public SyncBlueprintTECapability(@Nonnull BlockPos blockPos, Level world) {
        AtomicReference<CompoundTag> tag = new AtomicReference<>();
        world.getBlockEntity(blockPos).getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY).ifPresent(iBluePrint -> {
            tag.set(CapabilityStoreHelper.storeBlueprintCapability(iBluePrint));
        });
        this.data = tag.get();
        this.blockPos = blockPos;
    }

    public SyncBlueprintTECapability(@Nonnull FriendlyByteBuf packetBuffer) {
        this.data = packetBuffer.readNbt();
        this.blockPos = packetBuffer.readBlockPos();
    }

    public void encode(@Nonnull FriendlyByteBuf buf) {
        buf.writeNbt(this.data);
        buf.writeBlockPos(this.blockPos);
    }

    public boolean handle(@Nonnull Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if(Minecraft.getInstance().level != null) {
                Minecraft.getInstance().level.getBlockEntity(this.blockPos).getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY).ifPresent(iBluePrint -> {
                    CapabilityStoreHelper.readBlueprintCapability(this.data, iBluePrint);
                });
            }
        });
        return true;
    }
}
