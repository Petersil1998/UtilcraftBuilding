package net.petersil98.utilcraft_building.network;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.petersil98.utilcraft_building.container.BlueprintBlockContainer;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public class CreateStructurePacket {

    public CreateStructurePacket() {}

    public CreateStructurePacket(@Nonnull PacketBuffer packetBuffer) {}

    public void encode(@Nonnull PacketBuffer buf) {}

    public boolean handle(@Nonnull Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if(player != null && player.containerMenu instanceof BlueprintBlockContainer) {
                ((BlueprintBlockContainer) player.containerMenu).createStructure();
            }
        });
        return true;
    }
}
