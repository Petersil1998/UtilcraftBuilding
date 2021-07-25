package net.petersil98.utilcraft_building.network;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fmllegacy.network.NetworkEvent;
import net.petersil98.utilcraft_building.data.capabilities.CapabilityStoreHelper;
import net.petersil98.utilcraft_building.data.capabilities.blueprint.CapabilityBlueprint;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class SyncBlueprintItemCapability {

    private final CompoundTag data;
    private final ItemStack itemStack;

    public SyncBlueprintItemCapability(@Nonnull ItemStack itemStack) {
        AtomicReference<CompoundTag> tag = new AtomicReference<>();
        itemStack.getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY).ifPresent(iBluePrint -> {
            tag.set(CapabilityStoreHelper.storeBlueprintCapability(iBluePrint));
        });
        this.data = tag.get();
        this.itemStack = itemStack;
    }

    public SyncBlueprintItemCapability(@Nonnull FriendlyByteBuf packetBuffer) {
        this.data = packetBuffer.readNbt();
        this.itemStack = packetBuffer.readItem();
    }

    public void encode(@Nonnull FriendlyByteBuf buf) {
        buf.writeNbt(this.data);
        buf.writeItem(this.itemStack);
    }

    public boolean handle(@Nonnull Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> this.itemStack.getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY).ifPresent(iBluePrint -> {
            CapabilityStoreHelper.readBlueprintCapability(this.data, iBluePrint);
        }));
        return true;
    }
}
