package net.petersil98.utilcraft_building.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fmllegacy.network.NetworkEvent;
import net.petersil98.utilcraft_building.container.BlueprintBlockContainer;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public class CreateStructurePacket {

    public CreateStructurePacket() {}

    public CreateStructurePacket(@Nonnull FriendlyByteBuf packetBuffer) {}

    public void encode(@Nonnull FriendlyByteBuf buf) {}

    public boolean handle(@Nonnull Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if(player != null && player.containerMenu instanceof BlueprintBlockContainer) {
                ((BlueprintBlockContainer) player.containerMenu).createStructure();
            }
        });
        return true;
    }
}
